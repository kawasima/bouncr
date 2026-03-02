FROM golang:1.25 AS builder
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 go build -o bouncr-proxy ./cmd/bouncr-proxy/

FROM gcr.io/distroless/static-debian12
COPY --from=builder /app/bouncr-proxy /
ENTRYPOINT ["/bouncr-proxy"]
