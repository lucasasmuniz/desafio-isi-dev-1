import type { DiscountDTO } from "./discount"

export type ProductDiscountDTO = {
    id:number,
    name:string,
    description:string,
    stock:number,
    isOutOfStock: boolean,
    price:number,
    finalPrice:number,
    discount:DiscountDTO,
    hasCouponApplied:boolean,
    createdAt:string,
    updatedAt:string
}