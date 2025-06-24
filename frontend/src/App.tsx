import { Navigate, Route, Routes } from 'react-router-dom'
import Layout from './routes'
import ProductsCatalog from './routes/ProductsCatalog'

function App() {

  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<Navigate replace to="/products" />} />
        <Route path="products" element={<ProductsCatalog />} />
      </Route>

      <Route path="*" element={<Navigate to="/" />} />
    </Routes>
  )
}

export default App
