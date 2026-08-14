
import { useQuery, useInfiniteQuery } from "react-query"
import api from "../apis/api"
import { getTodayDate } from "../utils/helper"

const PAGE_SIZE = 10;

// Server returns pages sorted most-recently-created-first, so pages can just be
// concatenated in fetch order without any client-side re-sorting.
export const useFetchMyShortUrls = (token, onError) => {
    return useInfiniteQuery("my-shortenurls",
         async ({ pageParam = 0 }) => {
            const { data } = await api.get(
                `/api/urls/myurls?page=${pageParam}&size=${PAGE_SIZE}`,
            {
                headers: {
                    "Content-Type": "application/json",
                    Accept: "application/json",
                    Authorization: "Bearer " + token,
                },
            }
        );
            return data;
    },
          {
            getNextPageParam: (lastPage) => (lastPage.last ? undefined : lastPage.page + 1),
            onError,
            staleTime: 5000
          }
        );
};

export const useFetchTotalClicks = (token, onError) => {
    return useQuery("url-totalclick",
         async () => {
            return await api.get(
                `/api/urls/totalClicks?startDate=2024-01-01&endDate=${getTodayDate()}`,
            {
                headers: {
                    "Content-Type": "application/json",
                    Accept: "application/json",
                    Authorization: "Bearer " + token,
                },
            }
        );
    },
          {
            select: (data) => {
                // data.data =>
                    //  {
                    //     "2024-01-01": 120,
                    //     "2024-01-02": 95,
                    //     "2024-01-03": 110,
                    //   };
                      
                const convertToArray = Object.keys(data.data).map((key) => ({
                    clickDate: key,
                    count: data.data[key], // data.data[2024-01-01]
                }));
                // Object.keys(data.data) => ["2024-01-01", "2024-01-02", "2024-01-03"]

                // FINAL:
                //   [
                //     { clickDate: "2024-01-01", count: 120 },
                //     { clickDate: "2024-01-02", count: 95 },
                //     { clickDate: "2024-01-03", count: 110 },
                //   ]
                return convertToArray;
            },
            onError,
            staleTime: 5000
          }
        );
};
