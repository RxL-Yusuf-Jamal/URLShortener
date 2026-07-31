
const TextField = ({
  label,
  id,
  type,
  errors,
  register,
  required,
  message,
  className,
  min,
  max,
  maxMessage,
  value,
  placeholder,
}) => {
  return (
    <div className="flex flex-col gap-1">
      <label
        htmlFor={id}
        className={`${className ? className : ""} font-semibold text-md text-slate-700 dark:text-neutral-200`}
      >
        {label}
      </label>

      <input
        type={type}
        id={id}
        placeholder={placeholder}
        className={`${
          className ? className : ""
        } px-2 py-2 border outline-none bg-transparent text-slate-800 dark:text-neutral-100 placeholder:text-slate-400 dark:placeholder:text-neutral-500 rounded-md focus:ring-2 focus:ring-brand-500/40 ${
          errors[id]?.message ? "border-red-500" : "border-slate-300 dark:border-neutral-600"
        }`}
        {...register(id, {
          required: { value: required, message },
          minLength: min
            ? { value: min, message: "Minimum 6 character is required" }
            : null,

          maxLength: max
            ? { value: max, message: maxMessage || `Maximum ${max} characters allowed` }
            : null,

          pattern:
            type === "email"
              ? {
                  value: /^[a-zA-Z0-9]+@(?:[a-zA-Z0-9]+\.)+com+$/,
                  message: "Invalid email",
                }
              : type === "url"
              ? {
                  value:
                    /^(https?:\/\/)?(([a-zA-Z0-9\u00a1-\uffff-]+\.)+[a-zA-Z\u00a1-\uffff]{2,})(:\d{2,5})?(\/[^\s]*)?$/,
                  message: "Please enter a valid url",
                }
              : null,
        })}
      />

      {errors[id]?.message && (
        <p className="text-sm font-semibold text-red-600 dark:text-red-400 mt-0">
          {errors[id]?.message}*
        </p>
      )}
    </div>
  );
};

export default TextField;
