import React from "react";
import { Outlet } from "react-router-dom";
import Navbar from "../components/Navbar";

const UserLayout = () => (
  <>
    <Navbar />
    <Outlet />
  </>
);

export default UserLayout;
