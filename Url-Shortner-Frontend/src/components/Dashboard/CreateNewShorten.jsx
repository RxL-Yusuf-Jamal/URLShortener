import { useState } from "react";
import { useStoreContext } from "../../contextApi/ContextApi";
import { useForm } from "react-hook-form";
import TextField from "../TextField";
import Tooltip from "../ui/Tooltip";
import Button from "../ui/Button";
import { HiOutlineXMark } from "react-icons/hi2";
import api from "../../apis/api";
import toast from "react-hot-toast";

const CreateNewShorten = ({ setOpen, refetch }) => {
  const { token } = useStoreContext();
  const [loading, setLoading] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({
    defaultValues: {
      originalUrl: "",
      expiresInDays: "",
    },
    mode: "onTouched",
  });

  const createShortUrlHandler = async (data) => {
    setLoading(true);
    try {
      const { data: res } = await api.post("/api/urls/shorten", data, {
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
          Authorization: "Bearer " + token,
        },
      });

      const shortenUrl = `${
        import.meta.env.VITE_REACT_FRONT_END_URL + "/s/" + `${res.shortUrl}`
      }`;
      navigator.clipboard.writeText(shortenUrl).then(() => {
        toast.success("Short URL Copied to Clipboard", {
          position: "bottom-center",
          className: "mb-5",
          duration: 3000,
        });
      });

      // REFRESH LIST
      if (refetch) await refetch();
      reset();
      setOpen(false);
    } catch (error) {
      const fieldError = error.response?.data?.errors?.originalUrl;
      const errorMessage = fieldError || error.response?.data?.error || "Create ShortURL Failed";
      toast.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex justify-center items-center bg-white dark:bg-neutral-900 rounded-lg">
      <form
        onSubmit={handleSubmit(createShortUrlHandler)}
        className="sm:w-[450px] w-[360px] relative shadow-custom dark:shadow-none border border-slate-200 dark:border-neutral-800 pt-8 pb-5 sm:px-8 px-4 rounded-lg"
      >
        <h1 className="font-montserrat sm:mt-0 mt-3 text-center font-bold sm:text-2xl text-[22px] text-slate-800 dark:text-white">
          Create a new shortened Url
        </h1>

        <hr className="mt-2 sm:mb-5 mb-3 border-slate-200 dark:border-neutral-800" />

        <div>
          <TextField
            label="Enter URL"
            required
            id="originalUrl"
            placeholder="https://example.com"
            type="url"
            message="Url is required"
            max={2048}
            maxMessage="URL cannot exceed 2048 characters"
            register={register}
            errors={errors}
          />
        </div>

        <div className="flex flex-col gap-1 mt-3">
          <label htmlFor="expiresInDays" className="font-semibold text-md text-slate-700 dark:text-neutral-200">
            Expires In
          </label>
          <select
            id="expiresInDays"
            className="px-2 py-2 border outline-none bg-transparent text-slate-700 dark:text-neutral-100 rounded-md border-slate-300 dark:border-neutral-600 focus:ring-2 focus:ring-brand-500/40"
            {...register("expiresInDays", {
              setValueAs: (value) => (value === "" ? null : Number(value)),
            })}
          >
            <option className="dark:bg-neutral-900" value="">Never</option>
            <option className="dark:bg-neutral-900" value="1">1 day</option>
            <option className="dark:bg-neutral-900" value="7">7 days</option>
            <option className="dark:bg-neutral-900" value="30">30 days</option>
            <option className="dark:bg-neutral-900" value="90">90 days</option>
          </select>
        </div>

        <Button variant="primary" className="w-32 my-3" type="submit" disabled={loading}>
          {loading ? "Loading..." : "Create"}
        </Button>

        {!loading && (
          <div className="absolute right-2 top-2">
            <Tooltip title="Close">
              <button
                disabled={loading}
                onClick={() => setOpen(false)}
                className="text-slate-500 dark:text-neutral-400 hover:text-slate-800 dark:hover:text-white transition-colors"
              >
                <HiOutlineXMark className="text-3xl" />
              </button>
            </Tooltip>
          </div>
        )}
      </form>
    </div>
  );
};

export default CreateNewShorten;
