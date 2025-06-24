import '../index.css';
import { Outlet } from "react-router-dom";
import Header from "../components/Header";
import SideBar from "../components/Sidebar";

export default function Layout(){
  return (
    <div className="app-container">
      <SideBar />
      <div className="main-content-wrapper">
        <Header />
        <main className="page-content main-container">
          <Outlet /> 
        </main>
      </div>
    </div>
  );
}