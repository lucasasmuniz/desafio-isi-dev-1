import './styles.css';
import editIcon from '../../assets/edit.svg';
import dollarIcon from '../../assets/dollar-sign.svg';
import trashIcon from '../../assets/trash-2.svg';
import type { ProductDiscountDTO } from '../../models/product';
import { useNavigate } from 'react-router-dom';
import * as productService from '../../service/product-service';

type Props = {
  product: ProductDiscountDTO;
  onDelete: Function;
}

export default function ProductTable({ product, onDelete }: Props) {
  const navigate = useNavigate()

  function handleDeleteProduct(id: number) {
    productService.deleteById(id)
      .then(() => {
        onDelete();
      });
  }

  return (
    <tr className='index-table'>
      <td>{product.name}</td>
      <td className="truncate-description">{product.description}</td>
      <td className="price-container">
        {
          product.discount !== null ?
            <>
              <div className="prices">
                <p className="price-line-through">R$ {product.price.toFixed(2)}</p>
                <p className="actual-price">R$ {product.finalPrice.toFixed(2)}</p>
              </div>
              {
                product.discount.type === "percent" ?
                  <span className="discount-badge">{product.discount.value.toFixed(2)}%</span> :
                  <span className="discount-badge">- R${product.discount.value.toFixed(2)}</span>
              }
            </>
            : <p className="actual-price">R$ {product.finalPrice.toFixed(2)}</p>
        }
      </td>
      {
        product.isOutOfStock ?
          <td>Esgotado</td> :
          <td>{product.stock}</td>
      }
      <td className="actions">
        <img onClick={() => navigate("/products/" + product.id)} src={editIcon} alt="Editar" />
        <img src={dollarIcon} alt="Cupom" />
        <img onClick={() => handleDeleteProduct(product.id)} src={trashIcon} alt="Deletar" />
      </td>
    </tr>
  );
}