"use client";

import { useState } from "react";
import { useCompany } from "@/features/companies/company-context";
import { useBranches } from "@/features/branches/api";
import { useCustomers } from "@/features/customers/api";
import { usePrescriptions, useCreatePrescription } from "@/features/prescriptions/api";
import { useHasPermission } from "@/features/auth/session";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label, Select } from "@/components/ui/input";
import { PageHeader, Spinner } from "@/components/ui/misc";

export default function PrescriptionsPage() {
  const { currentCompany } = useCompany();
  const hasPerm = useHasPermission();
  const { data: branches = [] } = useBranches(currentCompany?.id);
  const { data: customers = [] } = useCustomers();
  const [filterBranch, setFilterBranch] = useState("");
  const prescriptions = usePrescriptions(filterBranch || undefined);
  const createPrescription = useCreatePrescription();

  const [branchId, setBranchId] = useState("");
  const [customerId, setCustomerId] = useState("");
  const [doctorName, setDoctorName] = useState("");
  const [doctorLicense, setDoctorLicense] = useState("");
  const [issuedDate, setIssuedDate] = useState("");
  const [diagnosis, setDiagnosis] = useState("");
  const [medications, setMedications] = useState("");
  const [error, setError] = useState<string | null>(null);

  const submit = async () => {
    setError(null);
    try {
      await createPrescription.mutateAsync({
        branchId,
        customerId: customerId || undefined,
        doctorName,
        doctorLicense: doctorLicense || undefined,
        issuedDate,
        diagnosis: diagnosis || undefined,
        medications,
      });
      setDoctorName(""); setDoctorLicense(""); setIssuedDate(""); setDiagnosis(""); setMedications("");
    } catch (e) {
      setError((e as Error).message);
    }
  };

  return (
    <div>
      <PageHeader title="Recetas" subtitle="Registro de recetas medicas presentadas en la farmacia" />

      <div className="mb-4 max-w-xs">
        <Label>Filtrar por sucursal</Label>
        <Select value={filterBranch} onChange={(e) => setFilterBranch(e.target.value)}>
          <option value="">Todas</option>
          {branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
        </Select>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardContent className="p-5">
            <h2 className="mb-3 text-sm font-semibold">Recetas registradas</h2>
            {prescriptions.isLoading ? <Spinner /> : (prescriptions.data ?? []).length === 0 ? (
              <p className="text-sm text-muted-foreground">Aun no hay recetas registradas.</p>
            ) : (
              <ul className="divide-y divide-border text-sm">
                {(prescriptions.data ?? []).map((p) => (
                  <li key={p.id} className="py-2">
                    <div className="flex items-center justify-between">
                      <span className="font-medium">{p.doctorName}{p.doctorLicense ? ` (CMP ${p.doctorLicense})` : ""}</span>
                      <span className="text-muted-foreground">{p.issuedDate}</span>
                    </div>
                    <p className="mt-1 text-muted-foreground">
                      {p.customerName ? `Cliente: ${p.customerName} · ` : ""}{p.medications}
                    </p>
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>

        {hasPerm("prescription:manage") && (
          <Card>
            <CardContent className="space-y-3 p-5">
              <h2 className="text-sm font-semibold">Registrar receta</h2>
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1">
                  <Label>Sucursal</Label>
                  <Select value={branchId} onChange={(e) => setBranchId(e.target.value)}>
                    <option value="">Selecciona…</option>
                    {branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
                  </Select>
                </div>
                <div className="space-y-1">
                  <Label>Cliente (opcional)</Label>
                  <Select value={customerId} onChange={(e) => setCustomerId(e.target.value)}>
                    <option value="">—</option>
                    {customers.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                  </Select>
                </div>
                <div className="space-y-1"><Label>Medico</Label><Input value={doctorName} onChange={(e) => setDoctorName(e.target.value)} /></div>
                <div className="space-y-1"><Label>CMP</Label><Input value={doctorLicense} onChange={(e) => setDoctorLicense(e.target.value)} /></div>
                <div className="space-y-1"><Label>Fecha de emision</Label><Input type="date" value={issuedDate} onChange={(e) => setIssuedDate(e.target.value)} /></div>
                <div className="space-y-1"><Label>Diagnostico</Label><Input value={diagnosis} onChange={(e) => setDiagnosis(e.target.value)} /></div>
              </div>
              <div className="space-y-1">
                <Label>Medicamentos e indicaciones</Label>
                <textarea
                  className="w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-xs outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  rows={3}
                  value={medications}
                  onChange={(e) => setMedications(e.target.value)}
                  placeholder="Amoxicilina 500mg, 1 cada 8 horas por 7 dias..."
                />
              </div>
              {error && <p className="text-sm text-destructive">{error}</p>}
              <Button disabled={!branchId || !doctorName || !issuedDate || !medications || createPrescription.isPending} onClick={submit}>
                {createPrescription.isPending ? "Guardando..." : "Guardar receta"}
              </Button>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}
