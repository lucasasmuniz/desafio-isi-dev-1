import type { AxiosRequestConfig } from "axios";
import axios from "axios";
import { BASE_URL } from "./system";

export default function backendRequest(config: AxiosRequestConfig) {
  return axios({ ...config, baseURL: BASE_URL });
}