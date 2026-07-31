
import React, { useState } from 'react'
import { useForm } from 'react-hook-form'
import TextField from './TextField';
import Button from './ui/Button';
import { Link, useNavigate } from 'react-router-dom';
import api from '../apis/api';
import toast from 'react-hot-toast';

const RegisterPage = () => {
    const navigate = useNavigate();
    const [loader, setLoader] = useState(false);

    const {
        register,
        handleSubmit,
        reset,
        formState: {errors}
    } = useForm({
        defaultValues: {
            username: "",
            email: "",
            password: "",
        },
        mode: "onTouched",
    });

    const registerHandler = async (data) => {
        setLoader(true);
        try {
            const { data: response } = await api.post(
                "/api/auth/public/register",
                data
            );
            reset();
            navigate("/login");
            toast.success("Registeration Successful!")
        } catch (error) {
            console.log(error);
            const errorMessage = error.response?.data?.error || "Registration Failed!";

            toast.error(errorMessage);
        } finally {
            setLoader(false);
        }
    };

  return (
    <div
        className='min-h-[calc(100vh-64px)] flex justify-center items-center'>
        <form onSubmit={handleSubmit(registerHandler)}
            className="sm:w-[450px] w-[360px] bg-white dark:bg-neutral-900 border border-slate-200 dark:border-neutral-800 shadow-custom dark:shadow-none py-8 sm:px-8 px-4 rounded-md transition-colors duration-200">
            <h1 className="text-center text-brand-600 dark:text-brand-400 font-bold lg:text-3xl text-2xl">
                Register Here
            </h1>

            <hr className='mt-2 mb-5 border-slate-200 dark:border-neutral-800'/>

            <div className="flex flex-col gap-3">
                <TextField
                    label="UserName"
                    required
                    id="username"
                    type="text"
                    message="*Username is required"
                    placeholder="Type your username"
                    register={register}
                    errors={errors}
                />

                <TextField
                    label="Email"
                    required
                    id="email"
                    type="email"
                    message="*Email is required"
                    placeholder="Type your email"
                    register={register}
                    errors={errors}
                />

                <TextField
                    label="Password"
                    required
                    id="password"
                    type="password"
                    message="*Password is required"
                    placeholder="Type your password"
                    register={register}
                    min={6}
                    errors={errors}
                />
            </div>

            <Button
                disabled={loader}
                type='submit'
                variant='primary'
                className='w-full my-3'>
                {loader ? "Loading..." : "Register"}
            </Button>

            <p className='text-center text-sm text-slate-600 dark:text-neutral-400 mt-6'>
                Already have an account?
                <Link
                    className='font-semibold underline hover:text-slate-900 dark:hover:text-white'
                    to="/login">
                        <span className='text-brand-600 dark:text-brand-400'> Login</span>
                </Link>
            </p>
        </form>
    </div>
  )
}

export default RegisterPage
