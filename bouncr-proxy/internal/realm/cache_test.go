package realm

import (
	"fmt"
	"testing"
)

// buildCache creates a Cache with the given realms without a database connection.
func buildCache(realms []*Realm) *Cache {
	m := make(map[string][]*Realm)
	for _, r := range realms {
		vp := r.Application.VirtualPath
		m[vp] = append(m[vp], r)
	}
	return &Cache{realmsByPath: m}
}

func app(id int64, virtualPath string) *Application {
	return &Application{ID: id, VirtualPath: virtualPath, PassTo: "http://backend:8080"}
}

func TestMatch_ExactVirtualPath(t *testing.T) {
	r := &Realm{ID: 1, URL: "admin", Application: app(1, "/bouncr")}
	c := buildCache([]*Realm{r})

	got := c.Match("/bouncr")
	if got == nil {
		t.Fatal("expected a match, got nil")
	}
	if got.ID != 1 {
		t.Errorf("got realm ID %d, want 1", got.ID)
	}
}

func TestMatch_VirtualPathPlusURL(t *testing.T) {
	r := &Realm{ID: 2, URL: "api", Application: app(2, "/bouncr")}
	c := buildCache([]*Realm{r})

	got := c.Match("/bouncr/api")
	if got == nil {
		t.Fatal("expected a match, got nil")
	}
	if got.ID != 2 {
		t.Errorf("got realm ID %d, want 2", got.ID)
	}
}

func TestMatch_RealmURLWithSlash(t *testing.T) {
	// realm.URL may contain "/" (e.g. "api/users"), matching Java RealmCache semantics.
	// path == virtualPath + "/" + realm.URL must hold as a whole, not via LastIndex split.
	r := &Realm{ID: 5, URL: "api/users", Application: app(5, "/bouncr")}
	c := buildCache([]*Realm{r})

	got := c.Match("/bouncr/api/users")
	if got == nil {
		t.Fatal("expected a match for realm.URL with slash, got nil")
	}
	if got.ID != 5 {
		t.Errorf("got realm ID %d, want 5", got.ID)
	}
	// A path that is only a prefix of virtualPath+"/"+url must not match
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

func TestMatch_PartialPrefixDoesNotMatch(t *testing.T) {
	r := &Realm{ID: 4, URL: "api", Application: app(5, "/bouncr")}
	c := buildCache([]*Realm{r})

	// "/bouncr/api/extra" should NOT match
	if got := c.Match("/bouncr/api/extra"); got != nil {
		t.Errorf("expected nil for partial prefix, got realm ID %d", got.ID)
	}
}

func TestMatch_RootPath(t *testing.T) {
	r := &Realm{ID: 5, URL: "api", Application: app(6, "/bouncr")}
	c := buildCache([]*Realm{r})

	if got := c.Match("/"); got != nil {
		t.Errorf("expected nil for root path, got realm ID %d", got.ID)
	}
}

// BenchmarkMatch_DistinctVirtualPaths measures Match() when each realm has its
// own virtualPath (best-case O(1) map lookup for Case 1, O(k) iteration for Case 2).
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
			// Last realm in the slice — worst case for per-virtualPath scan
			target := fmt.Sprintf("/app/realm%d", n-1)

			b.ResetTimer()
			for range b.N {
				_ = c.Match(target)
			}
		})
	}
}
