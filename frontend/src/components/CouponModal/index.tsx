import './styles.css'
import ButtonPrimary from "../ButtonPrimary";
import ButtonSecondary from "../ButtonSecondary";
import tagIcon from '../../assets/tag.svg';
import tagWhiteIcon from '../../assets/tag2.svg';
import FormInput from "../FormInput";

export default function CouponModal() {
    return (
        <div className="dialog-background">
            <div className="dialog-card">
                <div className='dialog-title'>
                    <h2><img src={tagIcon} alt="" />Aplicar Desconto</h2>
                    <p>Escolha como aplicar o desconto ao produto</p>
                </div>
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
                    <div>
                        <ButtonSecondary text={"% Percentual Direto"} />
                    </div>
                </div>
                <div className='dialog-input-container'>
                    <p>Código do Cupom</p>
                    <FormInput 
                    placeholder = "Digite o código do cupom"
                    />
                </div>
                <div className='dialog-coupons-container'>
                    <p>Cupons disponíveis</p>
                    <div className='dialog-coupons'>
                        <div>
                            <ButtonSecondary text={"SAVE10 (10%)"} />
                        </div>
                        <div>
                            <ButtonSecondary text={"SAVE10 (15%)"} />
                        </div>
                        <div>
                            <ButtonSecondary text={"SAVE10 (20%)"} />
                        </div>
                        <div>
                            <ButtonSecondary text={"SAVE10 (25%)"} />
                        </div>
                        <div>
                            <ButtonSecondary text={"SAVE10 (30%)"} />
                        </div>
                        <div>
                            <ButtonSecondary text={"SAVE10 (45%)"} />
                        </div>
                        <div>
                            <ButtonSecondary text={"SAVE10 (35%)"} />
                        </div>
                    </div>
                </div>
                <div className='dialog-footer'>
                    <ButtonSecondary text={"Cancelar"} />
                    <ButtonPrimary text={"Aplicar"} />
                </div>
            </div>
        </div>
    );
}