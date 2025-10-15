## Space API

API for an image/file hosting service, pastebin host, temp email and URL shortener.

## Links

- [API Docs](https://docs.xap3y.space)
- [Frontend](https://xap3y.space)
- [Frontend-source](https://github.com/xap3y/front-space-v2/)
- [Public API](https://call.xap3y.space/)
- [Status](https://uptime.xap3y.tech/status/space)

<br>

#### API Status
<img src="https://uptime.xap3y.tech/api/badge/22/status" alt="Uptime" />

## Dependencies

- [dcraw](https://archlinux.org/packages/extra/x86_64/dcraw/)
- [imagemagick](https://archlinux.org/packages/extra/x86_64/imagemagick/)
- Have S3 or R2 bucket
- MariaDB or MySQL database
- SMTP server
- [this cf worker to receive emails](https://github.com/xap3y/space-temp-mail-cf-worker)
- Telegram bot (optional)
- Discord bot (optional)
- [Tempo server](https://github.com/grafana/tempo) (for tracing, optional)
- [Loki server](https://github.com/grafana/loki) (for logging, optional)
- [OpenTelemetry Instrumentation for Java](https://github.com/open-telemetry/opentelemetry-java-instrumentation) (optional)
- [Prometheus](https://prometheus.io/) (for metrics, optional)
- [Grafana](https://grafana.com/) (for metrics, optional)
<br>

#### It took me

[![wakatime](https://wakatime.com/badge/user/018ed1c7-6d42-4752-b478-df3c0e773732/project/b017fb88-4a6f-41df-98d5-d08fa87d7678.svg)](https://wakatime.com/badge/user/018ed1c7-6d42-4752-b478-df3c0e773732/project/b017fb88-4a6f-41df-98d5-d08fa87d7678)