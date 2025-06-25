export type JsonPatchOp = {
  op: 'replace' | 'remove';
  path: string;
  value?: any;
};