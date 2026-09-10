"""Arranca el microservicio SUNAT (Flask).  Uso:  python run.py

Expone:
    GET /api/sunat/dni/<dni>?doc_type=1
    GET /api/sunat/ruc/<ruc>
    GET /health

NO ejecutes `python app/server.py` directo (rompe los imports relativos del paquete).
"""
import os

from app.server import app

if __name__ == "__main__":
    port = int(os.getenv("SUNAT_PORT", "8090"))
    # threaded=False: las consultas con Playwright corren en serie (evita choques de
    # event loop en Windows). Suficiente para uso interno.
    app.run(host="0.0.0.0", port=port, threaded=False)
