import editIcon from '../../assets/edit.svg';

import PageHeader from "../../components/PageHeader";
import ProductForm from "../../components/ProductForm";

export default function ProductUpdate(){
    return (
        <>
            <div>
                <PageHeader
                    title="Editar Produto"
                    icon={editIcon}
                />
            </div>
            <div>
                <ProductForm editing={true} />
            </div>
        </>
    );
}