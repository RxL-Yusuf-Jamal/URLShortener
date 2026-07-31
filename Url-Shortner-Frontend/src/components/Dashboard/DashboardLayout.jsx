
import React, { useState } from 'react'
import Graph from './Graph'
import { useStoreContext } from '../../contextApi/ContextApi'
import { useFetchMyShortUrls, useFetchTotalClicks } from '../../hooks/useQuery'
import ShortenPopUp from './ShortenPopUp'
import { HiOutlineLink } from 'react-icons/hi2'
import ShortenUrlList from './ShortenUrlList'
import { useNavigate } from 'react-router-dom'
import Loader from '../Loader'
import Button from '../ui/Button'

const DashboardLayout = () => {
    // const refetch = false;
    const { token } = useStoreContext();
    const navigate = useNavigate();
    const [shortenPopUp, setShortenPopUp] = useState(false);

    // console.log(useFetchTotalClicks(token, onError));

    const {
        isLoading,
        data: myShortenUrlPages,
        refetch,
        fetchNextPage,
        hasNextPage,
        isFetchingNextPage,
    } = useFetchMyShortUrls(token, onError)

    const myShortenUrls = myShortenUrlPages?.pages?.flatMap((page) => page.content) ?? []

    const {isLoading: loader, data: totalClicks} = useFetchTotalClicks(token, onError)

    function onError() {
      navigate("/error");
    }

  return (
    <div className="lg:px-14 sm:px-8 px-4 min-h-[calc(100vh-64px)]">
        {loader ? (
            <Loader />
        ): (
        <div className="lg:w-[90%] w-full mx-auto py-16">
            <div className=" h-96 relative ">
                {totalClicks.length === 0 && (
                     <div className="absolute flex flex-col  justify-center sm:items-center items-end  w-full left-0 top-0 bottom-0 right-0 m-auto">
                     <h1 className=" text-slate-800 dark:text-white font-serif sm:text-2xl text-[18px] font-bold mb-1">
                       No Data For This Time Period
                     </h1>
                     <h3 className="sm:w-96 w-[90%] sm:ml-0 pl-6 text-center sm:text-lg text-sm text-slate-600 dark:text-neutral-400 ">
                       Share your short link to view where your engagements are
                       coming from
                     </h3>
                   </div>
                )}
                <Graph graphData={totalClicks} />
            </div>
            <div className='py-5 sm:text-end text-center'>
                <Button variant="primary" onClick={() => setShortenPopUp(true)}>
                    Create a New Short URL
                </Button>
            </div>

            <div>
              {!isLoading && myShortenUrls.length === 0 ? (
                <div className="flex justify-center pt-16">
                  <div className="flex gap-2 items-center justify-center  py-6 sm:px-8 px-5 rounded-md shadow-lg bg-gray-50 dark:bg-neutral-900 border border-slate-200 dark:border-neutral-800">
                    <h1 className="text-slate-800 dark:text-white font-montserrat   sm:text-[18px] text-[14px] font-semibold mb-1 ">
                      You haven't created any short link yet
                    </h1>
                    <HiOutlineLink className="text-brand-500 dark:text-brand-400 sm:text-xl text-sm " />
                  </div>
              </div>
              ) : (
                  <>
                    <ShortenUrlList data={myShortenUrls} />
                    {hasNextPage && (
                      <div className="flex justify-center pb-10">
                        <Button
                          variant="secondary"
                          onClick={() => fetchNextPage()}
                          disabled={isFetchingNextPage}
                        >
                          {isFetchingNextPage ? "Loading..." : "Load More"}
                        </Button>
                      </div>
                    )}
                  </>
              )}
            </div>
        </div>
        )}

        <ShortenPopUp
          refetch={refetch}
          open={shortenPopUp}
          setOpen={setShortenPopUp}
        />
    </div>
  )
}

export default DashboardLayout
