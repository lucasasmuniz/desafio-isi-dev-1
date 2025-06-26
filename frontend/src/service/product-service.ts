import type { AxiosRequestConfig } from "axios";
import backendRequest from "../utils/requests";
import type { ProductDTO } from "../models/product";
import type { JsonPatchOp } from "../models/jsonPatch";

export function findPageRequest(
  minPrice: string,
  maxPrice: string,
  search: string,
  size: number = 10,
  sort: string = "price",
  page:number,
  hasDiscount: boolean
) {
  const params: any = {
    size,
    sort,
    page,
    ...(hasDiscount && { hasDiscount }),
    ...(minPrice && { minPrice }),
    ...(maxPrice && { maxPrice }),
    ...(search && { search }),
  };

  const config: AxiosRequestConfig = {
    method: "GET",
    url: "/products",
    params,
  };

  return backendRequest(config);
}

export function findById(id: number) {
  return backendRequest({ url: `/products/${id}`, method: "GET" });
}

export function deleteById(id: number) {
  const config: AxiosRequestConfig = {
    method: "DELETE",
    url: `/products/${id}`,
  };

  return backendRequest(config);
}

export function removeDiscountFromProduct(id: number) {
  const config: AxiosRequestConfig = {
    method: "DELETE",
    url: `/products/${id}/discount`,
  };

  return backendRequest(config);
}

export function updateProduct(jsonPatch: JsonPatchOp[], id: number) {
  const config: AxiosRequestConfig = {
    method: "PATCH",
    url: `/products/${id}`,
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
    url: `/products`,
    data:product
  };
  return backendRequest(config);
}

export function applyCouponToProduct(productId: number, code:string) {
  const data = { code };
  const config: AxiosRequestConfig = {
    method: "POST",
    url: `/products/${productId}/discount/coupon`,
    data:data
  };
  return backendRequest(config);
}

export function applyDirectDiscountToProduct(productId: number, percentage:number) {
  const data = { percentage };
  const config: AxiosRequestConfig = {
    method: "POST",
    url: `/products/${productId}/discount/percent`,
    data:data
  };
  return backendRequest(config);
}
