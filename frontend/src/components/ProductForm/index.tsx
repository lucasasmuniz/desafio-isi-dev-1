import './styles.css';
import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import ButtonPrimary from "../ButtonPrimary";
import ButtonSecondary from "../ButtonSecondary";
import FormInput from "../FormInput";
import FormTextArea from "../FormTextArea";
import * as forms from '../../utils/forms';
import * as productService from '../../service/product-service';
import { formatCurrencyInput, parseCurrencyBRL } from '../../utils/prices';

export default function ProductForm() {

    const navigate = useNavigate();
    const params = useParams();
    const isEditing = params.productId !== "create";

    const [formData, setFormData] = useState<any>({
        name: {
            value: "",
            id: "name",
            name: "name",
            type: "text",
            placeholder: "Informe o nome do produto",
            validation: function (name: string) {
                return name.length >= 3 && name.length <= 100;
            },
            message: "CAMPO OBRIGATÓRIO: Favor informar um nome de 3 a 100 caracteres"
        },
        description: {
            value: "",
            id: "description",
            name: "description",
            type: "string",
            placeholder: "Descrição detalhada do produto",
            validation: function (name: string) {
                return name.trim() === "" || name.length <= 300;
            },
            message: "Favor informar um descrição com menos de 300 caracteres"
        },
        price: {
            value: "",
            id: "price",
            name: "price",
            type: "string",
            placeholder: "R$ 0,00",
            validation: function (value: string) {
                const parsedValue = Number(parseCurrencyBRL(value))
                return !isNaN(parsedValue) && parsedValue >= 0;
            },
            message: "CAMPO OBRIGATÓRIO: Favor informar um valor positivo"
        },
        stock: {
            value: "",
            id: "stock",
            name: "stock",
            type: "string",
            placeholder: "0",
            validation: function (value: number) {
                return !isNaN(value) && value >= 0;
            },
            message: "CAMPO OBRIGATÓRIO: Favor informar um valor positivo"
        }
    })

    function handleCancel(event: any) {
        event.preventDefault();
        navigate("/products");
    }

    function handleOnChangeInput(event: any) {
        if (event.target.name === "price") {
            const numberPattern = /[0-9]+/;
            if(!numberPattern.test(event.target.value)){
                return;
            }
            const rawValue = event.target.value;

            const numericOnly = rawValue.replace(/\D/g, '');
            const formatted = formatCurrencyInput(numericOnly);

            event.target.value = formatted;
            setFormData(forms.updateAndValidate(formData, event.target.name, event.target.value));

        } else if (event.target.name === "stock") {
            const numberPattern = /^[0-9]+$/;
            if(!numberPattern.test(event.target.value)){
                return;
            }
            setFormData(forms.updateAndValidate(formData, event.target.name, Number(event.target.value)));

        } else {
            setFormData(forms.updateAndValidate(formData, event.target.name, event.target.value));

        }
    }

    function handleTurnDirty(name: string) {
        setFormData(forms.toDirtyAndValidate(formData, name));
    }

    function handleSubmit(event: any) {
        event.preventDefault();
        const newFormData = forms.toDirtyAndValidateAll(formData);
        if (forms.hasAnyInvalid(newFormData)) {
            setFormData(newFormData);
            return
        }

        const requestBody = forms.toValues(formData);

        const request = productService.saveProduct(requestBody);

        request
            .then(() => {
                navigate("/products");
            })
            .catch(error => {
                const newFormData = forms.setBackendErrors(formData, error.response.data.errors);
                setFormData(newFormData);
            })

    }

    return (
        <form className='product-form' action="">
            <div className='form-title'>
                <h3>Dados do produto</h3>
                <span>|</span>
                <h4>O campo abaixo é obrigatório para o cadastro.</h4>
            </div>
            <div className='form-body'>
                <div className='form-input-container'>
                    <span>Nome do produto </span>
                    <span className='required-field'>*</span>
                    <div className='mb-22'>
                        <FormInput
                            {...formData.name}
                            className="form-input"
                            onTurnDirty={handleTurnDirty}
                            onChange={handleOnChangeInput}
                        />
                        <p className="form-error">{formData.name.message}</p>
                    </div>
                </div>
                <div className='form-input-container'>
                    <span>Descrição </span>
                    <div className='mb-22'>
                        <FormTextArea
                            {...formData.description}
                            className="form-input"
                            onTurnDirty={handleTurnDirty}
                            onChange={handleOnChangeInput}
                        />
                        <p className="form-error">{formData.description.message}</p>
                    </div>
                </div>
                <div className='group-form-input'>
                    <div className='form-input-container'>
                        <span>Preço </span>
                        <span className='required-field'>*</span>
                        <div className='mb-22'>
                            <FormInput
                                {...formData.price}
                                className="form-input"
                                onTurnDirty={handleTurnDirty}
                                onChange={handleOnChangeInput}
                            />
                            <p className="form-error">{formData.price.message}</p>
                        </div>
                    </div>
                    <div className='form-input-container'>
                        <span>Estoque </span>
                        <span className='required-field'>*</span>
                        <div className='mb-22'>
                            <FormInput
                                {...formData.stock}
                                className="form-input"
                                onTurnDirty={handleTurnDirty}
                                onChange={handleOnChangeInput}
                            />
                            <p className="form-error">{formData.stock.message}</p>
                        </div>
                    </div>
                </div>
                <div className='form-button-container'>
                    <div onClick={handleCancel}>
                        <ButtonSecondary text={'Cancelar'} />
                    </div>
                    <div onClick={handleSubmit}>
                        <ButtonPrimary text={'Cadastrar'} />
                    </div>
                </div>
            </div>
        </form>
    );
}