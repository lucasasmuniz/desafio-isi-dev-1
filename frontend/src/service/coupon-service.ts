import type { AxiosRequestConfig } from "axios";
import backendRequest from "../utils/requests";

export function getAllValidCoupons(){
      const config: AxiosRequestConfig = {
        method: "GET",
        url: `/coupons?onlyValid=true`,
      };
    
      return backendRequest(config);
}