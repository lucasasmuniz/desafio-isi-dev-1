import './styles.css';
import registerIcon from '../../assets/file-plus-2.svg';
import PageHeader from '../../components/PageHeader';
import ProductForm from '../../components/ProductForm';

export default function ProductRegister() {
    return (
        <>
            <div>
                <PageHeader
                    title="Cadastro de Produto"
                    icon={registerIcon}
                />
            </div>
            <div>
                <ProductForm />
            </div>
        </>
    );
}