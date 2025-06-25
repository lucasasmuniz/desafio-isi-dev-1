import './styles.css'
import ButtonPrimary from "../ButtonPrimary";
import ButtonSecondary from "../ButtonSecondary";
import tagIcon from '../../assets/tag.svg';
import tagWhiteIcon from '../../assets/tag2.svg';
import FormInput from "../FormInput";

export default function DiscountFixedModal(){
    return(
        <div className="dialog-background">
            <div className="dialog-card">
                <div className='dialog-title'>
                    <h2><img src={tagIcon} alt="" />Aplicar Desconto</h2>
                    <p>Escolha como aplicar o desconto ao produto</p>
                </div>
                <div className='dialog-btn-container'>
                    <div>
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
                    <FormInput 
                    placeholder = "Ex: 10%"
                    />
                    <span>Digite um valor entre 1% e 80%</span>
                </div>
                <div className='dialog-footer'>
                    <ButtonSecondary text={"Cancelar"} />
                    <ButtonPrimary text={"Aplicar"} />
                </div>
            </div>
        </div>
    );
}