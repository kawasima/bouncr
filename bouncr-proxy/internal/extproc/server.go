package extproc

import (
	"context"
	"io"
	"log"

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
	log.Printf("ext_proc: new stream started")
	ctx := stream.Context()
	for {
		req, err := stream.Recv()
		if err == io.EOF {
			log.Printf("ext_proc: stream EOF")
			return nil
		}
		if err != nil {
			log.Printf("ext_proc: recv error: %v", err)
			return status.Errorf(codes.Internal, "recv error: %v", err)
		}

		var resp *extprocpb.ProcessingResponse

		switch v := req.Request.(type) {
		case *extprocpb.ProcessingRequest_RequestHeaders:
			log.Printf("ext_proc: received request_headers")
			resp = s.handleRequestHeaders(ctx, v)
		default:
			log.Printf("ext_proc: received non-header request type: %T", req.Request)
			// For any other phase (body, trailers, response), just continue
			resp = &extprocpb.ProcessingResponse{
				Response: &extprocpb.ProcessingResponse_RequestHeaders{
					RequestHeaders: &extprocpb.HeadersResponse{},
				},
			}
		}

		if err := stream.Send(resp); err != nil {
			log.Printf("ext_proc: send error: %v", err)
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
		return continueResponse(nil)
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
	log.Printf("ext_proc: path=%q", path)

	if path == "" {
		log.Printf("ext_proc: no :path header, rejecting")
		return immediateResponse(typev3.StatusCode_BadRequest)
	}

	result, err := s.authenticator.Authenticate(ctx, headerMap, path)
	if err != nil {
		log.Printf("ext_proc: authentication error: %v", err)
		return immediateResponse(typev3.StatusCode_ServiceUnavailable)
	}

	if result == nil {
		log.Printf("ext_proc: no realm match for path %q, rejecting", path)
		return immediateResponse(typev3.StatusCode_NotFound)
	}

	// Build header mutations
	var headers []*corev3.HeaderValueOption

	// 1. Path rewrite: set :path to the backend-facing path
	if result.RewritePath != "" && result.RewritePath != path {
		log.Printf("ext_proc: rewriting path %q -> %q", path, result.RewritePath)
		headers = append(headers, &corev3.HeaderValueOption{
			Header: &corev3.HeaderValue{
				Key:      ":path",
				RawValue: []byte(result.RewritePath),
			},
		})
	}

	// 2. Set cluster routing header (used by Envoy cluster_header)
	if result.ClusterName != "" {
		log.Printf("ext_proc: routing to cluster %q", result.ClusterName)
		headers = append(headers, &corev3.HeaderValueOption{
			Header: &corev3.HeaderValue{
				Key:      "x-bouncr-cluster",
				RawValue: []byte(result.ClusterName),
			},
		})
	}

	// 3. Add JWT credential header (if authenticated)
	if result.HeaderName != "" && result.HeaderValue != "" {
		log.Printf("ext_proc: adding credential header %s", result.HeaderName)
		headers = append(headers, &corev3.HeaderValueOption{
			Header: &corev3.HeaderValue{
				Key:      result.HeaderName,
				RawValue: []byte(result.HeaderValue),
			},
		})
	}

	return continueResponse(headers)
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

func continueResponse(headers []*corev3.HeaderValueOption) *extprocpb.ProcessingResponse {
	headersResp := &extprocpb.HeadersResponse{}
	if len(headers) > 0 {
		headersResp.Response = &extprocpb.CommonResponse{
			HeaderMutation: &extprocpb.HeaderMutation{
				SetHeaders: headers,
			},
		}
	}
	return &extprocpb.ProcessingResponse{
		Response: &extprocpb.ProcessingResponse_RequestHeaders{
			RequestHeaders: headersResp,
		},
	}
}
