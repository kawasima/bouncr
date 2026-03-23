package realm

import (
	"fmt"
	"regexp"
	"sort"
	"testing"
)

// buildCache creates a Cache with the given realms without a database connection.
// It compiles URLPattern from URL for each realm.
func buildCache(realms []*Realm) *Cache {
	m := make(map[string][]*Realm)
	for _, r := range realms {
		if r.URLPattern == nil {
			r.URLPattern = regexp.MustCompile("^" + r.URL + "$")
		}
		vp := r.Application.VirtualPath
		m[vp] = append(m[vp], r)
	}
	sortedVPs := make([]string, 0, len(m))
	for vp := range m {
		sortedVPs = append(sortedVPs, vp)
	}
	sort.Slice(sortedVPs, func(i, j int) bool {
		return len(sortedVPs[i]) > len(sortedVPs[j])
	})
	return &Cache{realmsByPath: m, sortedVirtualPaths: sortedVPs}
}

func app(id int64, virtualPath string) *Application {
	return &Application{ID: id, VirtualPath: virtualPath, PassTo: "http://backend:8080"}
}

func TestMatch_ExactVirtualPath_WildcardURL(t *testing.T) {
	// url=".*" matches empty remainder when path == virtualPath
	r := &Realm{ID: 1, URL: ".*", Application: app(1, "/bouncr")}
	c := buildCache([]*Realm{r})

	got := c.Match("/bouncr")
	if got == nil {
		t.Fatal("expected a match, got nil")
	}
	if got.ID != 1 {
		t.Errorf("got realm ID %d, want 1", got.ID)
	}
}

func TestMatch_WildcardURL_MatchesSubPaths(t *testing.T) {
	// url=".*" should match any sub-path under virtualPath
	r := &Realm{ID: 1, URL: ".*", Application: app(1, "/bouncr")}
	c := buildCache([]*Realm{r})

	for _, path := range []string{"/bouncr/api", "/bouncr/api/users", "/bouncr/api/users/123"} {
		got := c.Match(path)
		if got == nil {
			t.Errorf("expected a match for %q, got nil", path)
		} else if got.ID != 1 {
			t.Errorf("for %q: got realm ID %d, want 1", path, got.ID)
		}
	}
}

func TestMatch_ExactURL(t *testing.T) {
	// url="api" matches only "api", not "api/users"
	r := &Realm{ID: 2, URL: "api", Application: app(2, "/bouncr")}
	c := buildCache([]*Realm{r})

	got := c.Match("/bouncr/api")
	if got == nil {
		t.Fatal("expected a match, got nil")
	}
	if got.ID != 2 {
		t.Errorf("got realm ID %d, want 2", got.ID)
	}

	// Should NOT match sub-paths
	if got := c.Match("/bouncr/api/extra"); got != nil {
		t.Errorf("expected nil for /bouncr/api/extra, got realm ID %d", got.ID)
	}
}

func TestMatch_PrefixPattern(t *testing.T) {
	// url="api/.*" matches "api/users", "api/groups", etc.
	r := &Realm{ID: 3, URL: "api/.*", Application: app(3, "/bouncr")}
	c := buildCache([]*Realm{r})

	for _, path := range []string{"/bouncr/api/users", "/bouncr/api/groups"} {
		got := c.Match(path)
		if got == nil {
			t.Errorf("expected a match for %q, got nil", path)
		} else if got.ID != 3 {
			t.Errorf("for %q: got realm ID %d, want 3", path, got.ID)
		}
	}

	// "api/" alone should also match (remainder = "api/" matches "^api/.*$")
	if got := c.Match("/bouncr/api/"); got == nil {
		t.Error("expected a match for /bouncr/api/, got nil")
	}

	// Just "api" should NOT match "^api/.*$"
	if got := c.Match("/bouncr/api"); got != nil {
		t.Errorf("expected nil for /bouncr/api, got realm ID %d", got.ID)
	}
}

func TestMatch_RealmURLWithSlash(t *testing.T) {
	r := &Realm{ID: 5, URL: "api/users", Application: app(5, "/bouncr")}
	c := buildCache([]*Realm{r})

	got := c.Match("/bouncr/api/users")
	if got == nil {
		t.Fatal("expected a match for realm.URL with slash, got nil")
	}
	if got.ID != 5 {
		t.Errorf("got realm ID %d, want 5", got.ID)
	}
	if got := c.Match("/bouncr/api"); got != nil {
		t.Errorf("expected nil for partial match, got realm ID %d", got.ID)
	}
}

func TestMatch_NoMatch(t *testing.T) {
	r := &Realm{ID: 3, URL: "api", Application: app(3, "/bouncr")}
	c := buildCache([]*Realm{r})

	if got := c.Match("/other/path"); got != nil {
		t.Errorf("expected nil, got realm ID %d", got.ID)
	}
}

func TestMatch_MultipleRealmsUnderSameVirtualPath(t *testing.T) {
	a := app(4, "/app")
	r1 := &Realm{ID: 10, URL: "admin", Application: a}
	r2 := &Realm{ID: 11, URL: "user", Application: a}
	c := buildCache([]*Realm{r1, r2})

	if got := c.Match("/app/admin"); got == nil || got.ID != 10 {
		t.Errorf("expected realm 10, got %v", got)
	}
	if got := c.Match("/app/user"); got == nil || got.ID != 11 {
		t.Errorf("expected realm 11, got %v", got)
	}
}

func TestMatch_NestedVirtualPaths_LongestWins(t *testing.T) {
	r1 := &Realm{ID: 20, URL: "endpoint", Application: app(7, "/a")}
	r2 := &Realm{ID: 21, URL: "endpoint", Application: app(8, "/a/b")}
	c := buildCache([]*Realm{r1, r2})

	got := c.Match("/a/b/endpoint")
	if got == nil || got.ID != 21 {
		t.Errorf("expected realm 21 (longer virtualPath /a/b wins), got %v", got)
	}
	got = c.Match("/a/endpoint")
	if got == nil || got.ID != 20 {
		t.Errorf("expected realm 20 (/a match), got %v", got)
	}
}

func TestMatch_RootVirtualPath(t *testing.T) {
	r := &Realm{ID: 6, URL: "api", Application: app(9, "/")}
	c := buildCache([]*Realm{r})

	got := c.Match("/api")
	if got == nil {
		t.Fatal("expected a match for virtualPath='/' + url='api', got nil")
	}
	if got.ID != 6 {
		t.Errorf("got realm ID %d, want 6", got.ID)
	}
}

func TestMatch_TrailingSlashVirtualPath(t *testing.T) {
	r := &Realm{ID: 7, URL: "api", Application: app(10, "/bouncr/")}
	c := buildCache([]*Realm{r})

	got := c.Match("/bouncr/api")
	if got == nil {
		t.Fatal("expected a match for virtualPath='/bouncr/' + url='api', got nil")
	}
	if got.ID != 7 {
		t.Errorf("got realm ID %d, want 7", got.ID)
	}
}

func TestMatch_RootPath(t *testing.T) {
	r := &Realm{ID: 5, URL: "api", Application: app(6, "/bouncr")}
	c := buildCache([]*Realm{r})

	if got := c.Match("/"); got != nil {
		t.Errorf("expected nil for root path, got realm ID %d", got.ID)
	}
}

func TestMatch_ExactVirtualPath_NoWildcard(t *testing.T) {
	// url="admin" does NOT match empty remainder when path == virtualPath
	r := &Realm{ID: 1, URL: "admin", Application: app(1, "/bouncr")}
	c := buildCache([]*Realm{r})

	if got := c.Match("/bouncr"); got != nil {
		t.Errorf("expected nil for exact virtualPath with url='admin', got realm ID %d", got.ID)
	}
}

// BenchmarkMatch_DistinctVirtualPaths measures Match() when each realm has its
// own virtualPath.
func BenchmarkMatch_DistinctVirtualPaths(b *testing.B) {
	for _, n := range []int{10, 100, 1000} {
		b.Run(fmt.Sprintf("realms=%d", n), func(b *testing.B) {
			realms := make([]*Realm, n)
			for i := range realms {
				realms[i] = &Realm{
					ID:          int64(i),
					URL:         fmt.Sprintf("realm%d", i),
					Application: app(int64(i), fmt.Sprintf("/app%d", i)),
				}
			}
			c := buildCache(realms)
			target := fmt.Sprintf("/app%d/realm%d", n-1, n-1)

			b.ResetTimer()
			for range b.N {
				_ = c.Match(target)
			}
		})
	}
}

// BenchmarkMatch_SharedVirtualPath measures Match() worst case: many realms
// share the same virtualPath and the target realm is last in the slice.
func BenchmarkMatch_SharedVirtualPath(b *testing.B) {
	for _, n := range []int{10, 100, 1000} {
		b.Run(fmt.Sprintf("realms=%d", n), func(b *testing.B) {
			a := app(1, "/app")
			realms := make([]*Realm, n)
			for i := range realms {
				realms[i] = &Realm{
					ID:          int64(i),
					URL:         fmt.Sprintf("realm%d", i),
					Application: a,
				}
			}
			c := buildCache(realms)
			target := fmt.Sprintf("/app/realm%d", n-1)

			b.ResetTimer()
			for range b.N {
				_ = c.Match(target)
			}
		})
	}
}
