export function formatCurrencyInput(value: string): string {
  const numericValue = value.replace(/\D/g, "");

  const number = parseFloat(numericValue) / 100;

  return number.toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
    minimumFractionDigits: 2,
  });
}

export function parseCurrencyBRL(value: string): string {
  return value
    .replace(/[^\d,]/g, "")
    .replace(".", "")
    .replace(",", ".");
}

export function parseCurrencyInternacional(value: string): string {
  return Number(value).toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
    minimumFractionDigits: 2,
  });
}
