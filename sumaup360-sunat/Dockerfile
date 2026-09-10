# Imagen oficial de Playwright (trae Chromium + libs del navegador).
FROM mcr.microsoft.com/playwright/python:v1.49.1-jammy

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt \
    && python -m playwright install chromium

COPY app ./app
COPY run.py ./run.py

EXPOSE 8090
# Nota: SUNAT puede requerir navegador VISIBLE (WAF). En contenedor headless,
# usa SUNAT_PLAYWRIGHT_HEADLESS=true + xvfb si hiciera falta.
CMD ["python", "run.py"]
