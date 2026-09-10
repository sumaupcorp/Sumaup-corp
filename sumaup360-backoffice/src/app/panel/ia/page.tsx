"use client";

import { useEffect, useState } from "react";
import { useAiPrompt, useUpdateAiPrompt } from "@/features/persons/api";
import { Card, CardContent } from "@/components/ui/card";
import { PageHeader, Spinner } from "@/components/ui/misc";
import { Button } from "@/components/ui/button";
import { Input, Label } from "@/components/ui/input";

export default function IaPage() {
  const { data: cfg, isLoading } = useAiPrompt();
  const update = useUpdateAiPrompt();

  const [chatSystem, setChatSystem] = useState("");
  const [diagnosisSystem, setDiagnosisSystem] = useState("");
  const [taxiContext, setTaxiContext] = useState("");
  const [peyaContext, setPeyaContext] = useState("");
  const [servContext, setServContext] = useState("");
  const [temperature, setTemperature] = useState("0.4");
  const [maxTokens, setMaxTokens] = useState("700");

  useEffect(() => {
    if (cfg) {
      setChatSystem(cfg.chatSystem ?? "");
      setDiagnosisSystem(cfg.diagnosisSystem ?? "");
      setTaxiContext(cfg.taxiContext ?? "");
      setPeyaContext(cfg.peyaContext ?? "");
      setServContext(cfg.servContext ?? "");
      setTemperature(String(cfg.temperature));
      setMaxTokens(String(cfg.maxTokens));
    }
  }, [cfg]);

  function save() {
    update.mutate({
      chatSystem, diagnosisSystem, taxiContext, peyaContext, servContext,
      temperature: Number(temperature), maxTokens: Number(maxTokens),
    });
  }

  return (
    <div>
      <PageHeader
        title="Inteligencia Artificial"
        subtitle="Ajusta el cerebro de Suma: instrucciones base, contexto por tipo de trabajador y parametros."
      />

      {isLoading ? (
        <Card><CardContent className="p-5"><Spinner /></CardContent></Card>
      ) : (
        <div className="space-y-6">
          <Card>
            <CardContent className="p-5">
              <h2 className="mb-1 font-heading text-base font-semibold">Instrucciones base</h2>
              <p className="mb-4 text-sm text-muted-foreground">
                Definen como responde Suma en el chat y en el diagnostico. Se combinan con el contexto del caso
                y con los datos del usuario (nombre, DNI, RUC, plan, estadisticas).
              </p>
              <div className="space-y-4">
                <Area label="System del chat" value={chatSystem} onChange={setChatSystem} rows={5} />
                <Area label="System del diagnostico" value={diagnosisSystem} onChange={setDiagnosisSystem} rows={5} />
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-5">
              <h2 className="mb-1 font-heading text-base font-semibold">Contexto por tipo de trabajador</h2>
              <p className="mb-4 text-sm text-muted-foreground">
                Contexto extra que se agrega segun el caso del usuario, para respuestas mas precisas.
              </p>
              <div className="space-y-4">
                <Area label="Taxista" value={taxiContext} onChange={setTaxiContext} rows={3} />
                <Area label="Repartidor de delivery" value={peyaContext} onChange={setPeyaContext} rows={3} />
                <Area label="Profesional (recibos por honorarios)" value={servContext} onChange={setServContext} rows={3} />
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-5">
              <h2 className="mb-1 font-heading text-base font-semibold">Parametros</h2>
              <p className="mb-4 text-sm text-muted-foreground">
                Temperatura: 0 = preciso/consistente, 1 = mas creativo. Tokens: largo maximo de la respuesta.
              </p>
              <div className="flex flex-wrap items-end gap-4">
                <div>
                  <Label>Temperatura (0 - 2)</Label>
                  <Input type="number" step="0.1" min="0" max="2" value={temperature}
                    onChange={(e) => setTemperature(e.target.value)} className="w-32" />
                </div>
                <div>
                  <Label>Max tokens (100 - 4000)</Label>
                  <Input type="number" min="100" max="4000" value={maxTokens}
                    onChange={(e) => setMaxTokens(e.target.value)} className="w-32" />
                </div>
              </div>
            </CardContent>
          </Card>

          <div className="flex items-center gap-3">
            <Button onClick={save} disabled={update.isPending}>
              {update.isPending ? "Guardando…" : "Guardar cambios"}
            </Button>
            {update.isSuccess && <span className="text-sm text-green-600">Guardado</span>}
          </div>
        </div>
      )}
    </div>
  );
}

function Area({ label, value, onChange, rows }: {
  label: string; value: string; onChange: (v: string) => void; rows: number;
}) {
  return (
    <div>
      <Label>{label}</Label>
      <textarea
        value={value}
        rows={rows}
        onChange={(e) => onChange(e.target.value)}
        className="mt-1 w-full rounded-lg border border-border bg-white px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
      />
    </div>
  );
}
