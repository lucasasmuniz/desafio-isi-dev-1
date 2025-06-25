import type { AxiosRequestConfig } from "axios";
import backendRequest from "../utils/requests";
import type { ProductDiscountDTO, ProductDTO } from "../models/product";
import type { JsonPatchOp } from "../models/jsonPatch";

export function findPageRequest(
  minPrice: string,
  maxPrice: string,
  search: string,
  size: number = 10,
  sort: string = "price"
) {
  const params: any = {
    size,
    sort,
    ...(minPrice && { minPrice }),
    ...(maxPrice && { maxPrice }),
    ...(search && { search }),
  };

  const config: AxiosRequestConfig = {
    method: "GET",
    url: "/api/v1/products",
    params,
  };

  return backendRequest(config);
}

export function findById(id: number) {
  return backendRequest({ url: `/api/v1/products/${id}`, method: "GET" });
}

export function deleteById(id: number) {
  const config: AxiosRequestConfig = {
    method: "DELETE",
    url: `/api/v1/products/${id}`,
  };

  return backendRequest(config);
}

export function removeDiscountFromProduct(id: number) {
  const config: AxiosRequestConfig = {
    method: "DELETE",
    url: `/api/v1/products/${id}/discount`,
  };

  return backendRequest(config);
}

export function updateProduct(jsonPatch: JsonPatchOp[], id: number) {
  const config: AxiosRequestConfig = {
    method: "PATCH",
    url: `/api/v1/products/${id}`,
    data: jsonPatch,
    headers: {
    'Content-Type': 'application/json-patch+json'
  }
  };

  return backendRequest(config);
}

export function saveProduct(product: ProductDTO) {
  const config: AxiosRequestConfig = {
    method: "POST",
    url: `/api/v1/products`,
    data:product
  };
  return backendRequest(config);
}
