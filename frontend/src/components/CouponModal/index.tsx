import './styles.css'
import ButtonPrimary from "../ButtonPrimary";
import ButtonSecondary from "../ButtonSecondary";
import tagIcon from '../../assets/tag.svg';
import tagWhiteIcon from '../../assets/tag2.svg';
import { useEffect, useState } from 'react';
import type { CouponDTO } from '../../models/coupon'
import * as couponService from '../../service/coupon-service'
import * as productService from '../../service/product-service';

type Props = {
    productId: number,
    onDialogAnswer: Function,
}

export default function CouponModal({ productId, onDialogAnswer }: Props) {
    const [buttonDirectDiscount, setButtonDirectDiscount] = useState(false);
    const [coupons, setCoupons] = useState<CouponDTO[]>([]);
    const [code, setCode] = useState("");
    const [value, setValue] = useState("");

    const [codeError, setCodeError] = useState("");
    const [valueError, setValueError] = useState("");

    useEffect(() => {
        couponService.getAllValidCoupons()
            .then((response) => {
                setCoupons(response.data)
            })
    }, [])

    function handleOnChange(event: React.ChangeEvent<HTMLInputElement>) {
        const { name, value } = event.target;

        if (name === "code") {
            setCode(value);
            if (value.trim() !== "") {
                setCodeError("");
            }
        } else if (name === "value") {
            const pattern = /^\d+(\.\d*)?$/;
            if (value === "" || pattern.test(value)) {
                setValue(value);
                setValueError(""); // limpa o erro se digitação válida
            }
        }
    }

    async function handleDiscount() {
        setCodeError("");
        setValueError("");

        if (buttonDirectDiscount) {
            if (code.trim() === "") {
                setCodeError("O código do cupom não pode estar vazio.");
                return;
            }

            try {
                await productService.applyCouponToProduct(productId, code);
                onDialogAnswer(true);
            } catch (error: any) {
                const backendMessage = error.response?.data?.message || "Erro ao aplicar cupom.";
                setCodeError(backendMessage);
            }

        } else {
            const numericValue = parseFloat(value);
            if (isNaN(numericValue) || numericValue < 1 || numericValue > 80) {
                setValueError("O valor deve estar entre 1% e 80%");
                return;
            }

            try {
                await productService.applyDirectDiscountToProduct(productId, numericValue);
                onDialogAnswer(true);
            } catch (error: any) {
                const backendMessage = error.response?.data?.message || "Erro ao aplicar desconto.";
                setValueError(backendMessage);
            }
        }
    }

    return (
        <div className="dialog-background" onClick={() => onDialogAnswer(false)}>
            <div className="dialog-card" onClick={(event) => event.stopPropagation()}>
                <div className='dialog-title'>
                    <h2><img src={tagIcon} alt="" />Aplicar Desconto</h2>
                    <p>Escolha como aplicar o desconto ao produto</p>
                </div>
                {
                    buttonDirectDiscount
                        ?
                        <>
                            <div className='dialog-btn-container'>
                                <div>
                                    <ButtonPrimary
                                        text={
                                            <>
                                                <img src={tagWhiteIcon} alt="" style={{ width: 16, marginRight: 8 }} />
                                                Código Cupom
                                            </>
                                        }
                                    />
                                </div>
                                <div onClick={() => setButtonDirectDiscount(false)}>
                                    <ButtonSecondary text={"% Percentual Direto"} />
                                </div>
                            </div>
                            <div className='dialog-input-container'>
                                <p>Código do Cupom</p>
                                <input
                                    onChange={handleOnChange}
                                    type="text"
                                    name='code'
                                    value={code}
                                    className={codeError ? 'input-error' : ''}
                                />
                                <h3 className={`error-message ${codeError ? 'show' : ''}`}>{codeError}</h3>
                            </div>
                            <div className='dialog-coupons-container'>
                                <p>Cupons disponíveis</p>
                                <div className='dialog-coupons'>
                                    {
                                        coupons.map(coupon => (
                                            <div onClick={() => setCode(coupon.code)} className='coupons' key={coupon.id}>
                                                <ButtonSecondary
                                                    text={
                                                        coupon.type === "percent"
                                                            ? `${coupon.code} (${coupon.value}%)`
                                                            : `${coupon.code} (-R$${coupon.value})`
                                                    }
                                                />
                                            </div>
                                        ))
                                    }
                                </div>
                            </div>
                        </>
                        :
                        <>
                            <div className='dialog-btn-container'>
                                <div onClick={() => setButtonDirectDiscount(true)} >
                                    <ButtonSecondary
                                        text={
                                            <>
                                                <img src={tagWhiteIcon} alt="" style={{ width: 16, marginRight: 8 }} />
                                                Código Cupom
                                            </>
                                        }
                                    />
                                </div>
                                <div>
                                    <ButtonPrimary text={"% Percentual Direto"} />
                                </div>
                            </div>
                            <div className='dialog-input-container'>
                                <p>Percentual de desconto</p>
                                <input
                                    onChange={handleOnChange}
                                    type="text"
                                    name='value'
                                    value={value}
                                    className={valueError ? 'input-error' : ''}
                                />
                                <h3 className={`error-message ${valueError ? 'show' : ''}`}>{valueError}</h3>
                                <span>Digite um valor entre 1% e 80%</span>
                            </div>
                        </>
                }

                <div className='dialog-footer'>
                    <div onClick={() => onDialogAnswer(false)}>
                        <ButtonSecondary text={"Cancelar"} />
                    </div>
                    <div onClick={handleDiscount}>
                        <ButtonPrimary text={"Aplicar"} />
                    </div>
                </div>
            </div>
        </div>
    );
}
