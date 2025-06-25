import './styles.css';
import editIcon from '../../assets/edit.svg';
import dollarIcon from '../../assets/dollar-sign.svg';
import trashIcon from '../../assets/trash-2.svg';

export default function ProductTable() {
  return (
    <table className="product-table">
      <thead>
        <tr>
          <th>Nome</th>
          <th>Descrição</th>
          <th>Preço</th>
          <th>Estoque</th>
          <th>Ações</th>
        </tr>
      </thead>
      <tbody>
        <tr className='index-table'>
          <td>Smartphone XYZ</td>
          <td className="truncate-description">Smartphone premium com ótimo custo-benefício e câmeras potentes.</td>
          <td className="price-container">
            <div className="prices">
              <p className="price-line-through">R$ 1999,99</p>
              <p className="actual-price">R$ 1799,99</p>
            </div>
              <span className="discount-badge">10%</span>
          </td>
          <td>50</td>
          <td className="actions">
            <img src={editIcon} alt="Editar" />
            <img src={dollarIcon} alt="Cupom" />
            <img src={trashIcon} alt="Deletar" />
          </td>
        </tr>
        <tr className='index-table'>
          <td>Smartphone XYZ</td>
          <td className="truncate-description">Smartphone premium com ótimo custo-benefício e câmeras potentes.</td>
          <td className="price-container">
            <p className="actual-price">R$ 1799,99</p>
          </td>
          <td>50</td>
          <td className="actions">
            <img src={editIcon} alt="Editar" />
            <img src={dollarIcon} alt="Cupom" />
            <img src={trashIcon} alt="Deletar" />
          </td>
        </tr>
      </tbody>
    </table>
  );
}