# Log-Intelligence-AI-Deploy

CLI tool for L3 production log triage: parse logs, mask sensitive values, summarize recurring errors, and request AI-assisted root-cause hints.

## Build

```powershell
Set-Location "E:\Log-Intelligence-AI-Deploy"
.\mvnw.cmd clean package
```

## Run

```powershell
java -jar .\target\log-intelligence-ai-deploy.jar --inputLog=.\sample-logs\sample.out --maskConfig=.\mask.properties --aiPrompt=.\prompt.txt
```

Required option:
- `--inputLog=<path>`

Optional options:
- `--maskConfig=<path to .properties>`
- `--aiPrompt=<path to .txt>`
- `--skipAiModelCheck=true` (skip startup `/api/tags` validation)

AI settings in `src/main/resources/application.yml`:
- `spring.ai.ollama.base-url`
- `spring.ai.ollama.chat.options.model`
- Optional custom prompt: `logai.ai.system-prompt`

The tool writes a sidecar report file at `<inputLog>.analysis.txt`.
