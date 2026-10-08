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
- `--skipAiModelCheck=true` (skip Ollama model validation via `/api/tags` at startup)

AI settings in `src/main/resources/application.yml`:
- `spring.ai.ollama.base-url`
- `spring.ai.ollama.chat.options.model`
- Optional custom system prompt: `logai.ai.system-prompt`

The tool writes a sidecar report file at `<inputLog>.analysis.txt`.
