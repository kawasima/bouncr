FROM golang:1.25 AS builder
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 go build -o bouncr-proxy ./cmd/bouncr-proxy/

FROM alpine:3
RUN apk add --no-cache wget
COPY --from=builder /app/bouncr-proxy /
ENTRYPOINT ["/bouncr-proxy"]
