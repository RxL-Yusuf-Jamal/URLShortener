import React from "react";
import {
  HiOutlineLink,
  HiOutlineShare,
  HiOutlineShieldCheck,
  HiOutlineBolt,
} from "react-icons/hi2";

const AboutPage = () => {
  return (
    <div className="lg:px-14 sm:px-8 px-5 min-h-[calc(100vh-64px)] flex justify-center items-center pt-2">
      <div className="bg-white dark:bg-neutral-950 w-full sm:py-10 py-8 max-w-5xl text-center transition-colors duration-200">
        <h1 className="sm:text-4xl text-slate-800 dark:text-white text-3xl font-bold italic mb-5">
          About SnapURL
        </h1>
        <p className="text-slate-600 dark:text-neutral-400 text-sm mb-8 xl:w-[70%] lg:w-[75%] sm:w-[80%] w-full mx-auto">
          SnapURL is a modern URL shortening platform that helps you create
          clean, memorable, and easy-to-share links in seconds. With powerful
          analytics, you can track clicks, user engagement, and traffic sources
          to better understand your audience. Designed for speed, reliability,
          and security, SnapURL ensures your links are always accessible and
          protected. Whether you’re an individual or a business, SnapURL makes
          link management simple and effective.
        </p>

        <div className="space-y-5 xl:w-[70%] lg:w-[75%] sm:w-[80%] w-full mx-auto text-left mb-10">
          <div className="flex items-start">
            <HiOutlineLink className="text-brand-500 dark:text-brand-400 text-3xl mr-4 flex-shrink-0" />
            <div>
              <h2 className="sm:text-2xl font-bold text-slate-800 dark:text-white mb-2">
                Simple URL Shortening
              </h2>
              <p className="text-slate-600 dark:text-neutral-400 text-sm">
                Experience the ease of creating short, memorable URLs in just a
                few clicks. Our intuitive interface and quick setup process
                ensure you can start shortening URLs without any hassle.
              </p>
            </div>
          </div>

          <div className="flex items-start">
            <HiOutlineShare className="text-emerald-500 dark:text-emerald-400 text-3xl mr-4 flex-shrink-0" />
            <div>
              <h2 className="sm:text-2xl font-bold text-slate-800 dark:text-white mb-2">
                Powerful Analytics
              </h2>
              <p className="text-slate-600 dark:text-neutral-400 text-sm">
                Gain insights into your link performance with our comprehensive
                analytics dashboard. Track clicks, geographical data, and
                referral sources to optimize your marketing strategies.
              </p>
            </div>
          </div>

          <div className="flex items-start">
            <HiOutlineShieldCheck className="text-purple-500 dark:text-purple-400 text-3xl mr-4 flex-shrink-0" />
            <div>
              <h2 className="sm:text-2xl font-bold text-slate-800 dark:text-white mb-2">
                Enhanced Security
              </h2>
              <p className="text-slate-600 dark:text-neutral-400 text-sm ">
                Rest assured with our robust security measures. All shortened
                URLs are protected with advanced encryption, ensuring your data
                remains safe and secure.
              </p>
            </div>
          </div>

          <div className="flex items-start">
            <HiOutlineBolt className="text-red-500 dark:text-red-400 text-3xl mr-4 flex-shrink-0" />
            <div>
              <h2 className="sm:text-2xl font-bold text-slate-800 dark:text-white mb-2">
                Fast and Reliable
              </h2>
              <p className="text-slate-600 dark:text-neutral-400 text-sm ">
                Enjoy lightning-fast redirects and high uptime with our reliable
                infrastructure. Your shortened URLs will always be available and
                responsive, ensuring a seamless experience for your users.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AboutPage;
