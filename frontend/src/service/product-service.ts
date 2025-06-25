import type { AxiosRequestConfig } from "axios";
import backendRequest from "../utils/requests";

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
  return backendRequest({ url: `/products/${id}`, method: "GET" });
}
