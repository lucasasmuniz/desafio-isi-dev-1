import type { AxiosRequestConfig } from "axios";
import backendRequest from "../utils/requests";

export function getAllValidCoupons(){
      const config: AxiosRequestConfig = {
        method: "GET",
        url: `/api/v1/coupons?onlyValid=true`,
      };
    
      return backendRequest(config);
}