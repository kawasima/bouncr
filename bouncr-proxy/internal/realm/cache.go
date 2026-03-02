package realm

import (
	"context"
	"fmt"
	"log"
	"regexp"
	"sync"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
)

type Cache struct {
	pool         *pgxpool.Pool
	mu           sync.RWMutex
	realms       []*Realm
	applications []*Application
}

func NewCache(dsn string) (*Cache, error) {
	pool, err := pgxpool.New(context.Background(), dsn)
	if err != nil {
		return nil, fmt.Errorf("connecting to database: %w", err)
	}
	c := &Cache{pool: pool}
	if err := c.Refresh(); err != nil {
		pool.Close()
		return nil, fmt.Errorf("initial realm load: %w", err)
	}
	return c, nil
}

func (c *Cache) StartPeriodicRefresh(interval time.Duration) {
	go func() {
		ticker := time.NewTicker(interval)
		defer ticker.Stop()
		for range ticker.C {
			if err := c.Refresh(); err != nil {
				log.Printf("realm cache refresh error: %v", err)
			}
		}
	}()
}

func (c *Cache) Refresh() error {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	rows, err := c.pool.Query(ctx, `
		SELECT a.application_id, a.virtual_path, a.pass_to,
		       r.realm_id, r.url
		FROM realms r
		JOIN applications a ON r.application_id = a.application_id
	`)
	if err != nil {
		return fmt.Errorf("querying realms: %w", err)
	}
	defer rows.Close()

	var realms []*Realm
	appMap := make(map[int64]*Application)
	for rows.Next() {
		var appID int64
		var virtualPath string
		var passTo string
		var realmID int64
		var realmURL string

		if err := rows.Scan(&appID, &virtualPath, &passTo, &realmID, &realmURL); err != nil {
			return fmt.Errorf("scanning realm row: %w", err)
		}

		app, ok := appMap[appID]
		if !ok {
			app = &Application{
				ID:          appID,
				VirtualPath: virtualPath,
				PassTo:      passTo,
			}
			appMap[appID] = app
		}

		// Pattern: ^<virtualPath>($|/<realmURL>)
		// Matches RealmCache.java line 73
		pattern := fmt.Sprintf("^%s($|/%s)", regexp.QuoteMeta(virtualPath), realmURL)
		compiled, err := regexp.Compile(pattern)
		if err != nil {
			log.Printf("invalid realm pattern %q: %v", pattern, err)
			continue
		}

		realms = append(realms, &Realm{
			ID:          realmID,
			URL:         realmURL,
			Application: app,
			PathPattern: compiled,
		})
	}
	if err := rows.Err(); err != nil {
		return fmt.Errorf("iterating realm rows: %w", err)
	}

	// Collect unique applications
	apps := make([]*Application, 0, len(appMap))
	for _, app := range appMap {
		apps = append(apps, app)
	}

	c.mu.Lock()
	c.realms = realms
	c.applications = apps
	c.mu.Unlock()

	log.Printf("realm cache refreshed: %d realm(s), %d application(s)", len(realms), len(apps))
	return nil
}

// Match finds the first realm whose pattern matches the given path.
func (c *Cache) Match(path string) *Realm {
	c.mu.RLock()
	defer c.mu.RUnlock()
	for _, r := range c.realms {
		if r.Matches(path) {
			return r
		}
	}
	return nil
}

// Applications returns a snapshot of all cached applications.
func (c *Cache) Applications() []*Application {
	c.mu.RLock()
	defer c.mu.RUnlock()
	result := make([]*Application, len(c.applications))
	copy(result, c.applications)
	return result
}

func (c *Cache) Close() {
	c.pool.Close()
}
