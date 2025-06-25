export type CouponDTO = {
    id: number,
    code:string,
    type:string,
    value:number,
    oneShot:boolean,
    maxUses:number,
    validFrom:string,
    validUntil:string
}

export type applyCouponDTO = {
    code:string
}