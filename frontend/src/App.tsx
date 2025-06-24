import { Route, Routes } from 'react-router-dom'
import Layout from './routes'

function App() {

  return (
    <Routes>
      <Route path="/" element={<Layout />}></Route>

      <Route path="*" element={<h1>Página não encontrada</h1>} />
    </Routes>
  )
}

export default App
