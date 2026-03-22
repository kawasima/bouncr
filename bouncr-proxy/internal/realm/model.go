package realm

import (
	"fmt"
	"net/url"
	"strings"
)

type Application struct {
	ID          int64
	VirtualPath string
	PassTo      string // Full URI (e.g., "http://api:3005/bouncr/api")
}

// ClusterName returns an Envoy cluster name derived from the pass_to host:port.
func (a *Application) ClusterName() string {
	u, err := url.Parse(a.PassTo)
	if err != nil {
		return "default"
	}
	host := u.Hostname()
	port := u.Port()
	if port == "" {
		if u.Scheme == "https" {
			port = "443"
		} else {
			port = "80"
		}
	}
	// Sanitize for Envoy cluster name: replace dots/colons with underscores
	name := strings.ReplaceAll(host, ".", "_") + "_" + port
	return name
}

// BackendPath returns the path component of pass_to (without trailing slash).
func (a *Application) BackendPath() string {
	u, err := url.Parse(a.PassTo)
	if err != nil {
		return ""
	}
	return strings.TrimRight(u.Path, "/")
}

// BackendAddress returns host:port for the backend.
func (a *Application) BackendAddress() (string, int, error) {
	u, err := url.Parse(a.PassTo)
	if err != nil {
		return "", 0, err
	}
	host := u.Hostname()
	port := u.Port()
	if port == "" {
		if u.Scheme == "https" {
			return host, 443, nil
		}
		return host, 80, nil
	}
	var p int
	_, err = fmt.Sscanf(port, "%d", &p)
	if err != nil {
		return "", 0, err
	}
	return host, p, nil
}

type Realm struct {
	ID          int64
	URL         string
	Application *Application
}
