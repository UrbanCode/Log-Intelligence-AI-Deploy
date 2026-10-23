# Log-Intelligence-AI-Deploy
AI Log Intelligence CLI tool for L3 support log triage: parse logs, mask sensitive values, summarize recurring errors and request AI-assisted root-cause hints.

## Build
### Windows
```powershell
.\mvnw.cmd clean package
```
### Unix / Linux / macOS
```bash
./mvnw clean package
```

## Run
### Windows
```powershell
java -jar log-intelligence-ai-deploy.jar --inputLog=sample.out --maskConfig=mask.properties --aiPrompt=prompt.txt
```
### Unix / Linux / macOS
```bash
java -jar log-intelligence-ai-deploy.jar --inputLog=sample.out --maskConfig=mask.properties --aiPrompt=prompt.txt
```
Required option:
- `--inputLog=<path>`

Optional options:
- `--maskConfig=<path to .properties>`
- `--aiPrompt=<path to .txt>`
- `--aiMaxErrors=<number>` (`> 0`: top N errors, `<= 0`: analyze all errors, default: `5`)
- `--skipAiModelCheck=true` (skip Ollama model validation via `/api/tags` at startup)

## Masking behavior
- The generated log summary and report keep the **raw log values**.
- Masking is applied only to the prompt sent to the AI model.
- AI prompt masking is controlled by `src/main/resources/application.yml` defaults and optional `--maskConfig` overrides.

Supported mask keys in `mask.properties`:
- `mask.ip_port`
- `mask.email`
- `mask.uuid`
- `mask.mac`
- `mask.jdbc`
- `mask.host`
- `mask.cn`
- `mask.id`
- `mask.fingerprint`

Example `mask.properties`:

```properties
mask.ip_port=true
mask.email=true
mask.uuid=true
mask.mac=true
mask.jdbc=true
mask.host=true
mask.cn=true
mask.id=true
mask.fingerprint=true
```

AI settings in `src/main/resources/application.yml`:
- `spring.ai.ollama.base-url`
- `spring.ai.ollama.chat.options.model`
- Optional AI masking defaults: `logai.mask.*`
- Optional extra system guidance appended to baseline guardrails: `logai.ai.system-prompt`

The tool writes a sidecar report file at `<inputLog>.analysis.txt`.
