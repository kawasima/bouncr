package realm

import (
	"context"
	"fmt"
	"log"
	"sort"
	"strings"
	"sync"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
)

type Cache struct {
	pool         *pgxpool.Pool
	mu           sync.RWMutex
	realmsByPath map[string][]*Realm // keyed by Application.VirtualPath
	// sortedVirtualPaths holds virtualPaths sorted by descending length so that
	// the longest (most specific) virtualPath is matched first when multiple
	// virtualPaths can be a prefix of the same request path (e.g. /a and /a/b).
	sortedVirtualPaths []string
	applications       []*Application
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

	realmsByPath := make(map[string][]*Realm)
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

		realmsByPath[virtualPath] = append(realmsByPath[virtualPath], &Realm{
			ID:          realmID,
			URL:         realmURL,
			Application: app,
		})
	}
	if err := rows.Err(); err != nil {
		return fmt.Errorf("iterating realm rows: %w", err)
	}

	// Build a slice of virtualPaths sorted by descending length so that the
	// most specific (longest) prefix wins when paths nest (e.g. /a/b before /a).
	sortedVPs := make([]string, 0, len(realmsByPath))
	for vp := range realmsByPath {
		sortedVPs = append(sortedVPs, vp)
	}
	sort.Slice(sortedVPs, func(i, j int) bool {
		return len(sortedVPs[i]) > len(sortedVPs[j])
	})

	// Collect unique applications
	apps := make([]*Application, 0, len(appMap))
	for _, app := range appMap {
		apps = append(apps, app)
	}

	c.mu.Lock()
	c.realmsByPath = realmsByPath
	c.sortedVirtualPaths = sortedVPs
	c.applications = apps
	c.mu.Unlock()

	realmCount := 0
	for _, rs := range realmsByPath {
		realmCount += len(rs)
	}
	log.Printf("realm cache refreshed: %d realm(s), %d application(s)", realmCount, len(apps))
	return nil
}

// Match finds the realm whose path matches the given request path.
// Matching semantics (equivalent to Java RealmCache.matchesPath):
//
//	path == virtualPath                   → exact application match
//	path == virtualPath + "/" + realm.URL → realm sub-path match
//
// When multiple virtualPaths are prefixes of path, the longest (most specific)
// virtualPath wins, making the result deterministic.
func (c *Cache) Match(path string) *Realm {
	c.mu.RLock()
	defer c.mu.RUnlock()

	// Case 1: path == virtualPath (exact application match)
	if realms, ok := c.realmsByPath[path]; ok && len(realms) > 0 {
		return realms[0]
	}

	// Case 2: path == virtualPath + "/" + realm.URL
	// Iterate in descending virtualPath length order so the most specific prefix
	// wins. realm.URL may itself contain "/" (e.g. "api/users"), so we compare
	// the full remainder rather than splitting on the last "/".
	for _, vp := range c.sortedVirtualPaths {
		var prefix string
		if strings.HasSuffix(vp, "/") {
			prefix = vp
		} else {
			prefix = vp + "/"
		}
		if !strings.HasPrefix(path, prefix) {
			continue
		}
		realmURL := path[len(prefix):]
		for _, r := range c.realmsByPath[vp] {
			if r.URL == realmURL {
				return r
			}
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
