import { Navigate, Route, Routes } from 'react-router-dom'
import Layout from './routes'
import ProductsCatalog from './routes/ProductsCatalog'
import ProductRegister from './routes/ProductRegister'

function App() {

  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<Navigate replace to="/products" />} />
        <Route path="products" element={<ProductsCatalog />} />
        <Route path="products/new" element={<ProductRegister />} />
      </Route>

      <Route path="*" element={<Navigate to="/" />} />
    </Routes>
  )
}

export default App
