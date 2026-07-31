
import React from 'react'
import { HiOutlineExclamationTriangle } from 'react-icons/hi2'
import { useNavigate } from 'react-router-dom'
import Button from './ui/Button'

const ErrorPage = ({ message }) => {
    const navigate = useNavigate();
  return (
    <div className="flex flex-col items-center justify-center min-h-[calc(100vh-64px)] bg-white dark:bg-neutral-950 transition-colors duration-200 p-6">
        <HiOutlineExclamationTriangle className='text-6xl text-red-500 dark:text-red-400 mb-4' />
        <h1 className='text-3xl font-bold mb-2 text-slate-800 dark:text-white'>
            Oops! Something went wrong.
        </h1>
        <p className='text-slate-600 dark:text-neutral-400 mb-6 text-center'>
            {message ? message : "An unexpected error has occured"}
        </p>
        <Button variant="primary" onClick={() => navigate("/")}>
            Go back to home
        </Button>
    </div>
  )
}

export default ErrorPage
