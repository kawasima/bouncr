package envoyconf

import (
	"fmt"
	"io"
	"strings"

	"github.com/kawasima/bouncr/bouncr-proxy/internal/realm"
)

// GenerateClusters writes Envoy cluster YAML definitions for all applications.
// The output can be included in envoy.yaml's static_resources.clusters section.
func GenerateClusters(w io.Writer, apps []*realm.Application) error {
	for _, app := range apps {
		host, port, err := app.BackendAddress()
		if err != nil {
			return fmt.Errorf("parsing pass_to for app %d (%s): %w", app.ID, app.PassTo, err)
		}

		clusterName := app.ClusterName()

		fmt.Fprintf(w, `    - name: %s
      type: STRICT_DNS
      lb_policy: ROUND_ROBIN
      load_assignment:
        cluster_name: %s
        endpoints:
          - lb_endpoints:
              - endpoint:
                  address:
                    socket_address:
                      address: %s
                      port_value: %d
`,
			clusterName, clusterName, host, port)
	}
	return nil
}

// GenerateFullConfig writes a complete envoy.yaml with dynamic clusters from DB
// and ext_proc integration.
func GenerateFullConfig(w io.Writer, apps []*realm.Application, extProcPort int, listenPort int) error {
	// Build cluster YAML for applications
	var clusterBuf strings.Builder
	if err := GenerateClusters(&clusterBuf, apps); err != nil {
		return err
	}

	tmpl := `static_resources:
  listeners:
    - name: main_listener
      address:
        socket_address:
          address: 0.0.0.0
          port_value: %d
      filter_chains:
        - filters:
            - name: envoy.filters.network.http_connection_manager
              typed_config:
                "@type": type.googleapis.com/envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager
                stat_prefix: ingress_http
                codec_type: AUTO
                route_config:
                  name: local_route
                  virtual_hosts:
                    - name: backend
                      domains: ["*"]
                      routes:
                        - match:
                            prefix: "/_healthcheck"
                          direct_response:
                            status: 200
                            body:
                              inline_string: "OK"
                        - match:
                            prefix: "/"
                          route:
                            cluster_header: "x-bouncr-cluster"
                            timeout: 30s
                http_filters:
                  - name: envoy.filters.http.ext_proc
                    typed_config:
                      "@type": type.googleapis.com/envoy.extensions.filters.http.ext_proc.v3.ExternalProcessor
                      grpc_service:
                        envoy_grpc:
                          cluster_name: ext_proc_cluster
                        timeout: 1s
                      processing_mode:
                        request_header_mode: SEND
                        response_header_mode: SKIP
                        request_body_mode: NONE
                        response_body_mode: NONE
                        request_trailer_mode: SKIP
                        response_trailer_mode: SKIP
                      failure_mode_allow: false
                  - name: envoy.filters.http.router
                    typed_config:
                      "@type": type.googleapis.com/envoy.extensions.filters.http.router.v3.Router

  clusters:
%s
    - name: ext_proc_cluster
      type: STRICT_DNS
      lb_policy: ROUND_ROBIN
      typed_extension_protocol_options:
        envoy.extensions.upstreams.http.v3.HttpProtocolOptions:
          "@type": type.googleapis.com/envoy.extensions.upstreams.http.v3.HttpProtocolOptions
          explicit_http_config:
            http2_protocol_options: {}
      load_assignment:
        cluster_name: ext_proc_cluster
        endpoints:
          - lb_endpoints:
              - endpoint:
                  address:
                    socket_address:
                      address: 127.0.0.1
                      port_value: %d

admin:
  address:
    socket_address:
      address: 127.0.0.1
      port_value: 9901
`

	fmt.Fprintf(w, tmpl, listenPort, clusterBuf.String(), extProcPort)
	return nil
}
