# Build stage
FROM golang:1.25-alpine AS builder

WORKDIR /build

# Download modules first for better layer caching.
COPY go.mod ./
RUN go mod download

COPY . .

RUN CGO_ENABLED=0 GOOS=linux \
    go build -ldflags="-s -w" -o webhook ./cmd/main.go

# -------------------------------------------------------------------
# Final stage – distroless (no shell, no package manager, non-root)
# -------------------------------------------------------------------
FROM gcr.io/distroless/static-debian12:nonroot

COPY --from=builder /build/webhook /webhook

EXPOSE 8443

ENTRYPOINT ["/webhook"]
