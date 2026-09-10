# Imagen oficial de Playwright (trae Chromium + libs del navegador).
FROM mcr.microsoft.com/playwright/python:v1.49.1-jammy

WORKDIR /app

# xvfb: permite navegador VISIBLE (SUNAT_PLAYWRIGHT_HEADLESS=false) en un servidor sin
# pantalla. El WAF F5 de SUNAT bloquea headless desde datacenter; visible bajo Xvfb pasa mejor.
RUN apt-get update && apt-get install -y --no-install-recommends xvfb \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt \
    && python -m playwright install chromium

COPY app ./app
COPY run.py ./run.py

EXPOSE 8090
# Corre bajo Xvfb para soportar navegador visible dentro del contenedor.
CMD ["xvfb-run", "-a", "--server-args=-screen 0 1280x1024x24", "python", "run.py"]
