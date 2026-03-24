package realm

import (
	"context"
	"fmt"
	"log/slog"
	"regexp"
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
				slog.Error("realm cache refresh failed", "error", err)
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

		urlPattern, err := regexp.Compile("^" + realmURL + "$")
		if err != nil {
			return fmt.Errorf("compiling realm URL pattern %q: %w", realmURL, err)
		}

		realmsByPath[virtualPath] = append(realmsByPath[virtualPath], &Realm{
			ID:          realmID,
			URL:         realmURL,
			URLPattern:  urlPattern,
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
	slog.Info("realm cache refreshed", "realms", realmCount, "applications", len(apps))
	return nil
}

// Match finds the realm whose path matches the given request path.
// Two-stage matching:
//
//  1. Stage 1: Find the application by longest virtualPath prefix match.
//  2. Stage 2: Match the remainder (path after virtualPath + "/") against
//     each realm's URLPattern (compiled as ^{url}$).
//
// When path == virtualPath exactly, the remainder is "" which matches url ".*".
func (c *Cache) Match(path string) *Realm {
	c.mu.RLock()
	defer c.mu.RUnlock()

	for _, vp := range c.sortedVirtualPaths {
		if path == vp {
			// Exact virtualPath match — remainder is ""
			for _, r := range c.realmsByPath[vp] {
				if r.URLPattern.MatchString("") {
					return r
				}
			}
			return nil
		}

		var prefix string
		if strings.HasSuffix(vp, "/") {
			prefix = vp
		} else {
			prefix = vp + "/"
		}
		if !strings.HasPrefix(path, prefix) {
			continue
		}
		remainder := path[len(prefix):]
		for _, r := range c.realmsByPath[vp] {
			if r.URLPattern.MatchString(remainder) {
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
