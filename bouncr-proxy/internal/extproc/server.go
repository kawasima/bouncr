package extproc

import (
	"context"
	"io"
	"log/slog"
	"strings"

	corev3 "github.com/envoyproxy/go-control-plane/envoy/config/core/v3"
	extprocpb "github.com/envoyproxy/go-control-plane/envoy/service/ext_proc/v3"
	typev3 "github.com/envoyproxy/go-control-plane/envoy/type/v3"
	"github.com/kawasima/bouncr/bouncr-proxy/internal/auth"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

type Server struct {
	extprocpb.UnimplementedExternalProcessorServer
	authenticator *auth.Authenticator
}

func NewServer(authenticator *auth.Authenticator) *Server {
	return &Server{authenticator: authenticator}
}

func (s *Server) Process(stream extprocpb.ExternalProcessor_ProcessServer) error {
	slog.Debug("new stream started")
	ctx := stream.Context()
	for {
		req, err := stream.Recv()
		if err == io.EOF {
			slog.Debug("stream EOF")
			return nil
		}
		if err != nil {
			slog.Error("recv error", "error", err)
			return status.Errorf(codes.Internal, "recv error: %v", err)
		}

		var resp *extprocpb.ProcessingResponse

		switch v := req.Request.(type) {
		case *extprocpb.ProcessingRequest_RequestHeaders:
			resp = s.handleRequestHeaders(ctx, v)
		default:
			slog.Debug("received non-header request type", "type", req.Request)
			// For any other phase (body, trailers, response), just continue
			resp = &extprocpb.ProcessingResponse{
				Response: &extprocpb.ProcessingResponse_RequestHeaders{
					RequestHeaders: &extprocpb.HeadersResponse{},
				},
			}
		}

		if err := stream.Send(resp); err != nil {
			slog.Error("send error", "error", err)
			return status.Errorf(codes.Internal, "send error: %v", err)
		}
	}
}

func (s *Server) handleRequestHeaders(
	ctx context.Context,
	req *extprocpb.ProcessingRequest_RequestHeaders,
) *extprocpb.ProcessingResponse {
	hdrs := req.RequestHeaders.GetHeaders()
	if hdrs == nil {
		return continueResponse(nil, nil)
	}

	// Build a map of headers for the authenticator
	headerMap := make(map[string]string, len(hdrs.GetHeaders()))
	var path string
	for _, h := range hdrs.GetHeaders() {
		headerMap[h.GetKey()] = string(h.GetRawValue())
		if h.GetKey() == ":path" {
			path = string(h.GetRawValue())
		}
	}

	if path == "" {
		slog.Warn("no :path header, rejecting request")
		return immediateResponse(typev3.StatusCode_BadRequest)
	}

	result, err := s.authenticator.Authenticate(ctx, headerMap, path)
	if err != nil {
		slog.Error("authentication error", "path", path, "error", err)
		return immediateResponse(typev3.StatusCode_ServiceUnavailable)
	}

	if result == nil {
		slog.Debug("no realm match", "path", path)
		return immediateResponse(typev3.StatusCode_NotFound)
	}

	// Build header mutations
	var headers []*corev3.HeaderValueOption

	// 1. Path rewrite: set :path to the backend-facing path
	if result.RewritePath != "" && result.RewritePath != path {
		slog.Debug("rewriting path", "from", path, "to", result.RewritePath)
		headers = append(headers, &corev3.HeaderValueOption{
			Header: &corev3.HeaderValue{
				Key:   ":path",
				Value: result.RewritePath,
			},
			AppendAction: corev3.HeaderValueOption_APPEND_IF_EXISTS_OR_ADD,
		})
	}

	// 2. Set cluster routing header (used by Envoy cluster_header)
	if result.ClusterName != "" {
		slog.Debug("routing to cluster", "cluster", result.ClusterName)
		headers = append(headers, &corev3.HeaderValueOption{
			Header: &corev3.HeaderValue{
				Key:   "x-bouncr-cluster",
				Value: result.ClusterName,
			},
			AppendAction: corev3.HeaderValueOption_APPEND_IF_EXISTS_OR_ADD,
		})
	}

	// 3. Add JWT credential header (if authenticated)
	if result.HeaderName != "" && result.HeaderValue != "" {
		slog.Debug("adding credential header", "header", result.HeaderName, "len", len(result.HeaderValue))
		headers = append(headers, &corev3.HeaderValueOption{
			Header: &corev3.HeaderValue{
				Key:      result.HeaderName,
				RawValue: []byte(result.HeaderValue),
			},
			AppendAction: corev3.HeaderValueOption_APPEND_IF_EXISTS_OR_ADD,
		})
	}

	// Always strip client-supplied trusted headers to prevent header injection attacks.
	// A client could forge these headers and reach the backend if ext_proc does not
	// set them for the request (e.g. unauthenticated but realm-matched request).
	//
	// IMPORTANT: Only remove the credential header if we did NOT just set it.
	// Envoy processes RemoveHeaders before SetHeaders within the same HeaderMutation,
	// so including a header in both SetHeaders and RemoveHeaders causes the header to
	// be removed (the set is effectively a no-op).
	var trustedHeaders []string
	credHeader := strings.ToLower(s.authenticator.CredentialHeaderName())
	if result.HeaderName == "" {
		// Unauthenticated: strip any client-injected credential header
		trustedHeaders = append(trustedHeaders, credHeader)
	}
	trustedHeaders = append(trustedHeaders, "x-bouncr-signature")
	return continueResponse(headers, trustedHeaders)
}

func immediateResponse(code typev3.StatusCode) *extprocpb.ProcessingResponse {
	return &extprocpb.ProcessingResponse{
		Response: &extprocpb.ProcessingResponse_ImmediateResponse{
			ImmediateResponse: &extprocpb.ImmediateResponse{
				Status: &typev3.HttpStatus{Code: code},
			},
		},
	}
}

func continueResponse(headers []*corev3.HeaderValueOption, removeHeaders []string) *extprocpb.ProcessingResponse {
	headersResp := &extprocpb.HeadersResponse{}
	if len(headers) > 0 || len(removeHeaders) > 0 {
		headersResp.Response = &extprocpb.CommonResponse{
			ClearRouteCache: true,
			HeaderMutation: &extprocpb.HeaderMutation{
				SetHeaders:    headers,
				RemoveHeaders: removeHeaders,
			},
		}
	}
	return &extprocpb.ProcessingResponse{
		Response: &extprocpb.ProcessingResponse_RequestHeaders{
			RequestHeaders: headersResp,
		},
	}
}
