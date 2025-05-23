/*
 * Copyright © 2024 T-Bank
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.tbank.core.tid.error

/**
 * Information about errors received in request responses
 *
 * @author Stanislav Mukhametshin
 */
public class TidErrorMessage(

    /** Human readable message of api error */
    public val message: String?,

    /**
     * Error type returned after sending request to endpoints, you can find all error types
     * in [TidTokenErrorConstants], [TidTokenSignOutErrorConstants]
     */
    public val errorType: Int
)
