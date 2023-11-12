import { AxiosError, AxiosInstance } from 'axios';
/**
 *  Request a retry mechanism
 */

export class AxiosRetry {
  /**
   * retry
   */
  retry(AxiosInstance: AxiosInstance, error: AxiosError) {
    // @ts-ignore
    const { config } = error.response;
    const { waitTime, count } = config?.requestOptions?.retryRequest;
    config.__retryCount = config.__retryCount || 0;
    if (config.__retryCount >= count) {
      return Promise.reject(error);
    }
    config.__retryCount += 1;
    return this.delay(waitTime).then(() => AxiosInstance(config));
  }

  /**
   * delay
   */
  private delay(waitTime: number) {
    return new Promise((resolve) => setTimeout(resolve, waitTime));
  }
}
