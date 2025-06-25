import type { JsonPatchOp } from '../models/jsonPatch';
import type { ProductDTO } from '../models/product';
import * as price from './prices';

export function update(inputs: any, name: string, newValue: any) {
  return { ...inputs, [name]: { ...inputs[name], value: newValue } };
}

export function toValues(inputs: any) {
  const data: any = {};
  for (const name in inputs) {
    if(name === "price"){
      const parsedValue = price.parseCurrencyBRL(inputs[name].value)
      data[name] = parsedValue;
    } else{
      data[name] = inputs[name].value;
    }
  }
  return data;
}

export function generateJsonPatch(
  original: Partial<ProductDTO>,
  patched: Partial<ProductDTO>
): JsonPatchOp[] {
  const patch: JsonPatchOp[] = [];

  const keys = new Set([
    ...Object.keys(original),
    ...Object.keys(patched)
  ]) as Set<keyof ProductDTO>;

  keys.forEach((key) => {
    const originalValue = original[key];
    const patchedValue = patched[key];

    if (
      originalValue !== undefined &&
      (patchedValue === undefined || patchedValue === null)
    ) {
      patch.push({
        op: 'remove',
        path: `/${key}`
      });
    } else if (originalValue !== patchedValue) {
      patch.push({
        op: 'replace',
        path: `/${key}`,
        value: patchedValue
      });
    }
  });

  return patch;
}

export function updateAll(inputs: any, newValue: any) {
  const newInputs: any = {};
  for (const name in inputs) {
    if(name === "price"){
      newInputs[name] = { ...inputs[name], value: price.parseCurrencyInternacional((newValue[name]).toString()) };
    } else {
    newInputs[name] = { ...inputs[name], value: newValue[name] };
    }
  }
  return newInputs;
}

export function validade(inputs: any, name: string) {
  if (!inputs[name].validation) {
    return inputs;
  }

  const isInvalid = !inputs[name].validation(inputs[name].value);

  return {
    ...inputs,
    [name]: { ...inputs[name], invalid: isInvalid.toString() },
  };
}

export function toDirty(inputs: any, name: string) {
  return { ...inputs, [name]: { ...inputs[name], dirty: "true" } };
}

export function updateAndValidate(inputs: any, name: string, newValue: any) {
  const dataUpdated = update(inputs, name, newValue);
  return validade(dataUpdated, name);
}

export function toDirtyAndValidate(inputs: any, name: string) {
  const dataDirty = { ...inputs, [name]: { ...inputs[name], dirty: "true" } };
  return validade(dataDirty, name);
}

export function toDirtyAll(inputs: any) {
  const newInputs: any = {};
  for (const name in inputs) {
    newInputs[name] = { ...inputs[name], dirty: "true" };
  }
  return newInputs;
}

export function validateAll(inputs: any) {
  const newInputs: any = {};

  for (const name in inputs) {
    if (inputs[name].validation) {
      const isInvalid = !inputs[name].validation(inputs[name].value);
      newInputs[name] = { ...inputs[name], invalid: isInvalid.toString() };
    } else {
      newInputs[name] = { ...inputs[name] };
    }
  }

  return newInputs;
}

export function toDirtyAndValidateAll(inputs: any){
  return validateAll(toDirtyAll(inputs));
}

export function hasAnyInvalid(inputs : any){
  for (const name in inputs){
    if(inputs[name].dirty === "true" && inputs[name].invalid === "true"){
      return true;
    }
  }
  return false;
}

export function setBackendErrors(inputs: any, errors: any[]){
  const newInputs = {...inputs};
  errors.forEach(item => {
    newInputs[item.fieldName].message = item.message;
    newInputs[item.fieldName].dirty = "true";
    newInputs[item.fieldName].invalid = "true";
  })

  return newInputs;
}